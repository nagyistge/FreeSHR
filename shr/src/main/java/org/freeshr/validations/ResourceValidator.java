package org.freeshr.validations;


import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.ResourceType;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResourceValidator implements Validator<Bundle> {

    public static final String INVALID = "invalid";
    public static final String CODE_UNKNOWN = "code-unknown";

    private Map<ResourceType, Validator<BundleEntryComponent>> resourceTypeValidatorMap = new HashMap<>();

    @Autowired
    public ResourceValidator(ConditionValidator conditionValidator,
                             MedicationPrescriptionValidator medicationPrescriptionValidator,
                             Validator<BundleEntryComponent> immunizationValidator,Validator<BundleEntryComponent> procedureValidator) {
        assignDefaultValidatorToAllResourceTypes();
        resourceTypeValidatorMap.put(ResourceType.Condition, conditionValidator);
        resourceTypeValidatorMap.put(ResourceType.MedicationPrescription, medicationPrescriptionValidator);
        resourceTypeValidatorMap.put(ResourceType.Immunization, immunizationValidator);
        resourceTypeValidatorMap.put(ResourceType.Procedure, procedureValidator);

    }

    private void assignDefaultValidatorToAllResourceTypes() {
        for (ResourceType resourceType : ResourceType.values()) {
            resourceTypeValidatorMap.put(resourceType, new DefaultValidator());
        }
    }

    @Override
    public List<ValidationMessage> validate(Bundle bundle) {
        List<ValidationMessage> validationMessages = new ArrayList<>();

        for (final BundleEntryComponent entry : bundle.getEntry()) {
            Validator<BundleEntryComponent> validator =
                    resourceTypeValidatorMap.get(entry.getResource().getResourceType());
            validationMessages.addAll(validator.validate(entry));
        }
        return validationMessages;
    }

}
