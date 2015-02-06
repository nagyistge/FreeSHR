package org.freeshr.validations;


import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.freeshr.domain.ErrorMessageBuilder.*;

@Component
public class HealthIdValidator implements Validator<EncounterValidationContext> {

    public HealthIdValidator() {
    }

    @Override
    public List<ValidationMessage> validate(EncounterValidationContext validationContext) {
        Bundle bundle = validationContext.getBundle();
        String expectedHealthId = validationContext.getHealthId();
        List<ValidationMessage> validationMessages = new ArrayList<>();
        for (BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();
            ResourceType resourceType = resource.getResourceType();
            Property subject = resource.getChildByName("subject");
            if(subject == null) {
                subject = resource.getChildByName("patient");
            }
            if(subject == null) {
                validationMessages.add(buildValidationMessage("healthId", "invalid", HEALTH_ID_NOT_PRESENT, OperationOutcome.IssueSeverity.ERROR));
                continue;
            }
            if (resourceType.equals(ResourceType.Composition) && !subject.hasValues()) {
                validationMessages.add(buildValidationMessage("healthId", "invalid", HEALTH_ID_NOT_PRESENT_IN_COMPOSITION, OperationOutcome.IssueSeverity.ERROR));
                return validationMessages;
            }
            if (!subject.hasValues()) continue;
            String healthIdFromUrl = getHealthIdFromUrl(((Reference) subject.getValues().get(0)).getReference());
            if (expectedHealthId.equals(healthIdFromUrl)) continue;

            validationMessages.add(buildValidationMessage("healthId", "invalid", HEALTH_ID_NOT_MATCH, OperationOutcome.IssueSeverity.ERROR));
        }

        return validationMessages;
    }

    private String getHealthIdFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1, url.length());
    }
}
