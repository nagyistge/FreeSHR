package org.freeshr.validations;


import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.model.Property;
import org.hl7.fhir.instance.model.Quantity;
import org.hl7.fhir.instance.utils.ITerminologyServices;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.freeshr.domain.ErrorMessageBuilder.INVALID_DOSAGE_QUANTITY;
import static org.freeshr.domain.ErrorMessageBuilder.buildValidationMessage;

@Component
public class ImmunizationValidator implements Validator<BundleEntryComponent> {

    private static final Logger logger = LoggerFactory.getLogger(ImmunizationValidator.class);
    public static final String DOSE_QUANTITY = "doseQuantity";


    private DoseQuantityValidator doseQuantityValidator;
    private UrlValidator urlValidator;


    @Autowired
    public ImmunizationValidator(DoseQuantityValidator doseQuantityValidator, UrlValidator urlValidator) {
        this.doseQuantityValidator = doseQuantityValidator;
        this.urlValidator = urlValidator;
    }

    @Override
    public List<ValidationMessage> validate(BundleEntryComponent entry) {
        return validateDosageQuantity(entry);
    }

    private List<ValidationMessage> validateDosageQuantity(BundleEntryComponent entry) {
        List<ValidationMessage> validationMessages = new ArrayList<>();
        String id = entry.getId();
        Property property = entry.getResource().getChildByName(DOSE_QUANTITY);

        if (!property.getName().equals(DOSE_QUANTITY) || !property.hasValues())
            return validationMessages;

        Quantity doseQuantity = (Quantity) property.getValues().get(0);

        if (!doseQuantityValidator.isReferenceUrlNotFound(doseQuantity) && urlValidator.isValid(doseQuantity.getSystem())) {
            ITerminologyServices.ValidationResult validationResult = doseQuantityValidator.validate(doseQuantity);
            if (validationResult != null) {
                logger.error("Medication-Prescription DosageQuantity Code is invalid:");

                validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, INVALID_DOSAGE_QUANTITY, OperationOutcome.IssueSeverity.ERROR));
            }
        }

        return validationMessages;
    }


}
