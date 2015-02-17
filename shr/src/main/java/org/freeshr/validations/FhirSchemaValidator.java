package org.freeshr.validations;

import org.apache.commons.io.IOUtils;
import org.freeshr.application.fhir.TRConceptLocator;
import org.freeshr.config.SHRProperties;
import org.hl7.fhir.instance.formats.JsonParser;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ValueSet;
import org.hl7.fhir.instance.utils.WorkerContext;
import org.hl7.fhir.instance.validation.InstanceValidator;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.AsyncRestTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import rx.Observable;
import rx.functions.Func1;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.freeshr.utils.HttpUtil.basicAuthHeaders;

@Component
public class FhirSchemaValidator implements Validator<String> {

    private final InstanceValidator instanceValidator;
    private final SHRProperties shrProperties;
    private AsyncRestTemplate shrRestTemplate;

    @Autowired
    public FhirSchemaValidator(TRConceptLocator trConceptLocator, SHRProperties shrProperties,
                               @Qualifier("fhirTrMap") Properties fhirTrMap, AsyncRestTemplate shrRestTemplate) throws Exception {
        this.shrProperties = shrProperties;
        this.shrRestTemplate = shrRestTemplate;
        //TODO inject profiles if required
        WorkerContext workerContext = WorkerContext.fromPack(shrProperties.getValidationFilePath());
        loadValueSets(workerContext, fhirTrMap);
        workerContext.setTerminologyServices(trConceptLocator);
        this.instanceValidator = new InstanceValidator(workerContext);
    }

    private Document document(String sourceXml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(sourceXml.getBytes()));
    }


    @Override
    public List<ValidationMessage> validate(String sourceXml) {
        try {
            return instanceValidator.validate(document(sourceXml).getDocumentElement());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ValueSet loadValueSet(String fhirValueSetUrl, String trValueSetUrl) {

        Observable<ValueSet> map = get(trValueSetUrl).map(new Func1<ResponseEntity<String>, ValueSet>() {
            @Override
            public ValueSet call(ResponseEntity<String> stringResponseEntity) {
                try {

                    Resource resource = new JsonParser().parse(IOUtils.toInputStream(stringResponseEntity.getBody(),
                            "UTF-8"));
                    return (ValueSet) resource;
                } catch (Exception e) {
                    return null;
                }
            }
        });

        map.onErrorReturn(new Func1<Throwable, ValueSet>() {
            @Override
            public ValueSet call(Throwable throwable) {
                return null;
            }
        });

        return hackId(fhirValueSetUrl, map.toBlocking().first());
    }

    private ValueSet hackId(String fhirValueSetUrl, ValueSet valueSet) {
        valueSet.setIdentifier(fhirValueSetUrl);
        valueSet.getDefine().setSystem(fhirValueSetUrl);
        return valueSet;
    }

    private Observable<ResponseEntity<String>> get(String uri) {
        return Observable.from(shrRestTemplate.exchange(uri,
                HttpMethod.GET,
                new HttpEntity(basicAuthHeaders(shrProperties.getTrUser(), shrProperties.getTrPassword())),
                String.class));
    }

    private void loadValueSets(WorkerContext workerContext, Properties fhirTrMap) {
        for (Object fhirValueSetUrl : fhirTrMap.keySet()) {
            String trValueSetUrl = (String) fhirTrMap.get(fhirValueSetUrl);
            workerContext.seeValueSet("http://hl7.org/fhir", loadValueSet((String) fhirValueSetUrl, trValueSetUrl));
        }
    }
}
