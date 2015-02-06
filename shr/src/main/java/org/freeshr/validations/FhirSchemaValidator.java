package org.freeshr.validations;

import org.freeshr.application.fhir.TRConceptLocator;
import org.hl7.fhir.instance.utils.WorkerContext;
import org.hl7.fhir.instance.validation.InstanceValidator;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Component
public class FhirSchemaValidator implements Validator<String> {

    private final InstanceValidator instanceValidator;

    @Autowired
    public FhirSchemaValidator(TRConceptLocator trConceptLocator) throws Exception {
        //TODO inject profiles if required
        WorkerContext workerContext = WorkerContext.fromClassPath();
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
}
