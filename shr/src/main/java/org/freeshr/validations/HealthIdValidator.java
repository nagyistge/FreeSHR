package org.freeshr.validations;


import org.freeshr.config.SHRProperties;
import org.freeshr.utils.StringUtils;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.freeshr.validations.ValidationMessages.HEALTH_ID_NOT_MATCH;

@Component
public class HealthIdValidator implements Validator<EncounterValidationContext> {

    private static final Logger logger = LoggerFactory.getLogger(HealthIdValidator.class);
    private SHRProperties shrProperties;
    //match all urls that have /api/*/patients, 2nd groups contains the variable
    private Pattern healthIdReferencePattern = Pattern.compile("(.+\\/api\\/)(\\w+)(\\/patients\\/.+)");

    @Autowired
    public HealthIdValidator(SHRProperties shrProperties) {
        this.shrProperties = shrProperties;
    }

    @Override
    public List<ValidationMessage> validate(EncounterValidationContext validationContext) {
        Bundle bundle = validationContext.getBundle();
        String expectedHealthId = validationContext.getHealthId();
        List<ValidationMessage> validationMessages = new ArrayList<>();
        for (BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();
            Property patientReference = getPatientReference(resource);

            boolean isPatientReferencePresent = hasValue(patientReference);

            if (isPatientReferencePresent) {
                Reference subjectRef = (Reference) patientReference.getValues().get(0);
                String healthIdFromUrl = validateAndIdentifyPatientId(subjectRef.getReference(), expectedHealthId);
                if (healthIdFromUrl == null) {
                    logger.debug(String.format("Encounter failed for %s", HEALTH_ID_NOT_MATCH));
                    validationMessages.add(new ValidationMessage(ValidationMessage.Source.ProfileValidator, ResourceValidator.INVALID, entry.getId(),
                            HEALTH_ID_NOT_MATCH, OperationOutcome.IssueSeverity.ERROR));
                }
            }

        }
        return validationMessages;
    }

    private Property getPatientReference(Resource resource) {
        Property patientReference = resource.getChildByName("patient");
        if (patientReference == null) {
            patientReference = resource.getChildByName("subject");
        }
        return patientReference;
    }

    private String validateAndIdentifyPatientId(String patientUrl, String healthId) {
        String expectedUrl = StringUtils.ensureSuffix(shrProperties.getPatientReferencePath(), "/") + healthId;
        Matcher actual = healthIdReferencePattern.matcher(patientUrl);
        Matcher expected = healthIdReferencePattern.matcher(expectedUrl);

        if (!actual.find() || !expected.find() || actual.groupCount() != 3) return null;
        if (expected.group(1).equalsIgnoreCase(actual.group(1)) && expected.group(3).equalsIgnoreCase(actual.group(3)))
            return actual.group(3);
        return null;
    }

    private boolean hasValue(Property property) {
        return (property != null) && property.hasValues();

    }

    private String getHealthIdFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1, url.length());
    }
}
