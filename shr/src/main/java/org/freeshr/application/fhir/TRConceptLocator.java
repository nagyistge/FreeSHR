package org.freeshr.application.fhir;

import org.apache.log4j.Logger;
import org.freeshr.config.SHRProperties;
import org.freeshr.infrastructure.tr.CodeValidator;
import org.freeshr.infrastructure.tr.CodeValidatorFactory;
import org.freeshr.utils.StringUtils;
import org.hl7.fhir.instance.model.CodeType;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.model.ValueSet;
import org.hl7.fhir.instance.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.instance.utils.ITerminologyServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.AsyncRestTemplate;
import rx.Observable;

@Component
public class TRConceptLocator implements ITerminologyServices {

    private final CodeValidatorFactory factory;
    private AsyncRestTemplate shrRestTemplate;
    private SHRProperties shrProperties;


    @Autowired
    public TRConceptLocator(CodeValidatorFactory factory, AsyncRestTemplate shrRestTemplate, SHRProperties shrProperties) {
        this.factory = factory;
        this.shrRestTemplate = shrRestTemplate;
        this.shrProperties = shrProperties;
    }


    private static Logger logger = Logger.getLogger(TRConceptLocator.class);

    @Override
    @Cacheable(value = "trCache", unless = "#result != null")
    public ValidationResult validateCode(String system, String code, String display) {
        if (getCodeDefinition(system, code) == null) {
            return new ValidationResult(OperationOutcome.IssueSeverity.ERROR, display);
        }
        return null;
    }

    @Override
    public ValueSetExpansionComponent expandVS(ValueSet.ConceptSetComponent inc) throws Exception {
        return null;
    }

    @Override
    public boolean supportsSystem(String system) {
        return factory.getValidator(system) != null;
    }

    @Override
    public ValueSet.ConceptDefinitionComponent getCodeDefinition(String system, String code) {
        try {
            final Boolean isValid = isValid(system, code).toBlocking().first();
            if (isValid) {
                CodeType codeType = new CodeType();
                codeType.setValue(code);
                return new ValueSet.ConceptDefinitionComponent(codeType);
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.warn(e);
            return null;
        }
    }

    @Override
    public ValueSet expandVS(ValueSet vs) throws Exception {
        return null;
    }

    @Override
    public boolean checkVS(ValueSet.ConceptSetComponent vsi, String system, String code) {
        return true;
    }

    @Override
    public boolean verifiesSystem(String system) {
        return factory.getValidator(system) != null;
    }

    public Observable<Boolean> isValid(String system, String code) {
        String trServerReferencePath = StringUtils.ensureSuffix(shrProperties.getTerminologyServerReferencePath(), "/");
        if (!system.startsWith(trServerReferencePath)) {
            return Observable.just(false);
        }

        String trLocationPath = StringUtils.ensureSuffix(shrProperties.getTRLocationPath(), "/");
        String terminologyRefSystem = system.replace(trServerReferencePath, trLocationPath);

        CodeValidator validator = factory.getValidator(terminologyRefSystem);
        if (validator != null) {
//            return validator.isValid(factory.getSystem(terminologyRefSystem), code);
            return validator.isValid(terminologyRefSystem, code);
        }
        return Observable.just(false);
    }
}
