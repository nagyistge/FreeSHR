package org.freeshr.validations;

import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.CodeableConcept;
import org.hl7.fhir.instance.model.Coding;
import org.hl7.fhir.instance.model.Property;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.hl7.fhir.instance.model.OperationOutcome.IssueSeverity;

@Component
public class ConditionValidator implements Validator<BundleEntryComponent> {
    private static final String CODEABLE_CONCEPT = "CodeableConcept";
    public static final String DIAGNOSIS = "Diagnosis";
    public static final String CATEGORY = "category";



    @Override
    public List<ValidationMessage> validate(BundleEntryComponent entry) {
        ArrayList<ValidationMessage> validationMessages = new ArrayList<>();
        for (Property property : entry.getResource().children()) {

            if (verifyIfPropertyIsARelatedItem(validationMessages, property, entry.getId())) continue;
            checkCodeableConcept(validationMessages, property, entry);
        }
        return validationMessages;

    }

    private boolean verifyIfPropertyIsARelatedItem(List<ValidationMessage> validationMessages, Property property,
                                                   String id) {
//        if (!property.getName().equals("relatedItem") || !(property.hasValues())) return false;
//
//        Condition.ConditionRelationshipType relatedItem = ((Condition.ConditionRelatedItemComponent) property
//                .getValues().get(0)).getTypeSimple();
//        Condition.ConditionRelationshipTypeEnumFactory conditionRelationshipTypeEnumFactory = new Condition
//                .ConditionRelationshipTypeEnumFactory();
//        try {
//            if (conditionRelationshipTypeEnumFactory.toCode(relatedItem).equals("?")) {
//                validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, UNKNOWN_CONDITION_RELATION_CODE, IssueSeverity.error));
//            }
//            return true;
//        } catch (Exception e) {
//            //Logically can never be thrown hence swallowing exception
//            e.printStackTrace();
//        }
        return true;
    }

    boolean skipCheckForThisTypeOfEntry(BundleEntryComponent entry) {
        Property category = entry.getResource().getChildByName(CATEGORY);
        Coding coding = ((CodeableConcept) category.getValues().get(0)).getCoding().get(0);
        return !coding.getDisplay().equalsIgnoreCase(DIAGNOSIS);
    }

    protected void checkCodeableConcept(List<ValidationMessage> validationMessages, Property property,
                                        BundleEntryComponent entry) {
        if (!property.getTypeCode().equals(CODEABLE_CONCEPT) || !property.hasValues() || skipCheckForThisTypeOfEntry
                (entry))
            return;

        boolean bothSystemAndCodePresent = bothSystemAndCodePresent(property);
        if (bothSystemAndCodePresent) return;

        String errorMessage = (((CodeableConcept) property.getValues().get(0)).getCoding()).get(0).getDisplay();
        ValidationMessage validationMessage = new ValidationMessage(null, ResourceValidator.CODE_UNKNOWN,
                entry.getId(), errorMessage, IssueSeverity.ERROR);
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
