package org.freeshr.application.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.IValidationSupport;
import org.freeshr.infrastructure.tr.TerminologyServer;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;


@Component
public class TRValidationSupport implements IValidationSupport {

    private final TerminologyServer terminologyServer;
    private final static Logger logger = LoggerFactory.getLogger(TRValidationSupport.class);

    @Autowired
    public TRValidationSupport(TerminologyServer terminologyServer) {
        this.terminologyServer = terminologyServer;
    }

    @Override
    public ValueSet.ValueSetExpansionComponent expandValueSet(ValueSet.ConceptSetComponent theInclude) {
        return null;
    }

    @Override
    public ValueSet fetchCodeSystem(String theSystem) {
        return null;
    }

    @Override
    public <T extends IBaseResource> T fetchResource(FhirContext theContext, Class<T> theClass, String theUri) {
        return null;
    }

    @Override
    public boolean isCodeSystemSupported(String theSystem) {
        return terminologyServer.verifiesSystem(StringUtils.trim(theSystem));
    }

    @Override
    public CodeValidationResult validateCode(String theCodeSystem, String theCode, String theDisplay) {
        CodeValidationResult result = locate(theCodeSystem, theCode, theDisplay);
        return result;
    }

    private CodeValidationResult locate(String system, String code, String display) {
        try {
            Boolean result = terminologyServer.isValid(system, code).toBlocking().first();
            if (result) {
                ValueSet.ConceptDefinitionComponent def = new ValueSet.ConceptDefinitionComponent();
                def.setDefinition(system);
                def.setCode(code);
                def.setDisplay(display);
                return new CodeValidationResult(def);
            } else {
                return new CodeValidationResult(OperationOutcome.IssueSeverity.ERROR,
                        String.format("Could not validate concept system[%s], code[%s]",system, code));
            }
        } catch (Exception e) {
            logger.error(String.format("Problem while validating concept system[%s], code[%s]",system, code), e);
            return new CodeValidationResult(OperationOutcome.IssueSeverity.ERROR, "Couldn't identify system and code. error:" + e.getMessage());
        }
    }

}
