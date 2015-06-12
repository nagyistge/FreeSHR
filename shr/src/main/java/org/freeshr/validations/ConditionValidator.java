package org.freeshr.validations;

import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Property;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.hl7.fhir.instance.model.OperationOutcome.IssueSeverity;

@Component
public class ConditionValidator implements Validator<Bundle.BundleEntryComponent> {

    private static final String CODEABLE_CONCEPT = "CodeableConcept";
    public static final String DIAGNOSIS = "Diagnosis";
    public static final String CATEGORY = "category";

    @Override
    public List<ValidationMessage> validate(Bundle.BundleEntryComponent entry) {
        ArrayList<ValidationMessage> validationMessages = new ArrayList<>();
        for (Property property : entry.getResource().children()) {
            if (hasRelatedItem(property)) continue;
            checkCodeableConcept(property, entry, validationMessages);
        }
        return validationMessages;

    }

    private boolean hasRelatedItem(Property property) {
        if(!property.hasValues()) return false;
        return property.getName().equals("dueTo") || property.getName().equals("occurredFollowing");
    }

    private boolean skipCheck(Bundle.BundleEntryComponent atomEntry) {
        Property category = atomEntry.getResource().getChildByName(CATEGORY);
        Coding coding = ((CodeableConcept) category.getValues().get(0)).getCoding().get(0);
        return !coding.getDisplay().equalsIgnoreCase(DIAGNOSIS);
    }

    private void checkCodeableConcept(Property property, Bundle.BundleEntryComponent atomEntry,
                                      List<ValidationMessage> validationMessages) {
        if (!property.getTypeCode().equals(CODEABLE_CONCEPT) || !property.hasValues() || skipCheck
                (atomEntry))
            return;

        if (bothSystemAndCodePresent(property)) return;

        String errorMessage = (((CodeableConcept) property.getValues().get(0)).getCoding()).get(0).getDisplay();

        ValidationMessage validationMessage = new ValidationMessage(null, ERROR_TYPE_CODE_UNKNOWN,
                atomEntry.getId(), errorMessage, IssueSeverity.ERROR);
        validationMessages.add(validationMessage);
    }

    private boolean bothSystemAndCodePresent(Property property) {
        boolean bothSystemAndCodePresent = false;
        List<Coding> codings = ((CodeableConcept) property.getValues().get(0)).getCoding();
        for (Coding coding : codings) {
            bothSystemAndCodePresent |= (coding.getSystem() != null && coding.getCode() != null);
        }
        return bothSystemAndCodePresent;
    }
}
