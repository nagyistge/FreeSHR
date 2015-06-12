package org.freeshr.validations;


import org.freeshr.infrastructure.tr.MedicationCodeValidator;
import org.hl7.fhir.instance.model.Base;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.MedicationPrescription;
import org.hl7.fhir.instance.model.Property;
import org.hl7.fhir.instance.model.Quantity;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.utils.ITerminologyServices;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.freeshr.validations.ValidationMessages.*;
import static org.hl7.fhir.instance.model.OperationOutcome.IssueSeverity.ERROR;

@Component
public class MedicationPrescriptionValidator implements Validator<BundleEntryComponent> {

    public static final String DOSAGE_INSTRUCTION = "dosageInstruction";
    private static final String MEDICATION = "medication";
    private static final String DISPENSE = "dispense";
    private static final Logger logger = LoggerFactory.getLogger(MedicationPrescriptionValidator.class);
    private MedicationCodeValidator codeValidator;
    private DoseQuantityValidator doseQuantityValidator;
    private UrlValidator urlValidator;

    @Autowired
    public MedicationPrescriptionValidator(MedicationCodeValidator codeValidator,
                                           DoseQuantityValidator doseQuantityValidator, UrlValidator urlValidator) {
        this.codeValidator = codeValidator;
        this.doseQuantityValidator = doseQuantityValidator;
        this.urlValidator = urlValidator;
    }

    @Override
    public List<ValidationMessage> validate(BundleEntryComponent bundleEntry) {

        List<ValidationMessage> validationMessages = new ArrayList<>();

        validationMessages.addAll(validateMedication(bundleEntry));
        if (validationMessages.size() > 0) return validationMessages;

        validationMessages.addAll(validateDosageQuantity(bundleEntry));
        if (validationMessages.size() > 0) return validationMessages;

        validationMessages.addAll(validateDispenseMedication(bundleEntry));
        return validationMessages;
    }

    private List<ValidationMessage> validateDosageQuantity(BundleEntryComponent bundleEntry) {
        Property dosageInstruction = bundleEntry.getResource().getChildByName(DOSAGE_INSTRUCTION);
        List<Base> dosageInstructionValues = dosageInstruction.getValues();
        for (Base dosageInstructionValue : dosageInstructionValues) {
            if (!(dosageInstructionValue instanceof MedicationPrescription
                    .MedicationPrescriptionDosageInstructionComponent)) continue;

            MedicationPrescription.MedicationPrescriptionDosageInstructionComponent dosageInstructionComponent = (MedicationPrescription.MedicationPrescriptionDosageInstructionComponent)
                    dosageInstructionValue;
            if (dosageInstructionComponent.hasDose() && dosageInstructionComponent.getDose() instanceof Quantity) {

                Quantity doseQuantity = (Quantity) dosageInstructionComponent.getDose();
                if (doseQuantityValidator.isReferenceUrlNotFound(doseQuantity)) return new ArrayList<>();
                if (!urlValidator.isValid(doseQuantity.getSystem())) {
                    logger.debug(String.format("Medication-Prescription:Encounter failed for %s", INVALID_DOSAGE_QUANTITY_REFERENCE));
                    return validationMessages(new ValidationMessage(null, ERROR_TYPE_INVALID, bundleEntry.getId(),
                            INVALID_DOSAGE_QUANTITY_REFERENCE, ERROR));
                }
                ITerminologyServices.ValidationResult validationResult = doseQuantityValidator.validate(doseQuantity);
                if (validationResult != null) {
                    logger.debug(String.format("Medication-Prescription:Encounter failed for %s", INVALID_DOSAGE_QUANTITY));
                    return validationMessages(new ValidationMessage(null, ERROR_TYPE_INVALID, bundleEntry.getId(),
                            INVALID_DOSAGE_QUANTITY, ERROR));
                }
            }

        }
        return new ArrayList<>();
    }


    private List<ValidationMessage> validateDispenseMedication(BundleEntryComponent bundleEntry) {
        Property dispense = bundleEntry.getResource().getChildByName(DISPENSE);
        if (dispense == null || (!dispense.hasValues())) return new ArrayList<>();
        Property dispenseMedication = dispense.getValues().get(0).getChildByName(MEDICATION);
        if (dispenseMedication == null || !dispenseMedication.hasValues()) return new ArrayList<>();
        String dispenseMedicationRefUrl = getReferenceUrl(dispenseMedication);
        if ((dispenseMedicationRefUrl == null)) return new ArrayList<>();
        if (!urlValidator.isValid(dispenseMedicationRefUrl))
            return validationMessages(new ValidationMessage(null, ERROR_TYPE_INVALID, bundleEntry.getId(),
                    INVALID_DISPENSE_MEDICATION_REFERENCE_URL, ERROR));
        if (!isValidCodeableConceptUrl(dispenseMedicationRefUrl, ""))
            return validationMessages(new ValidationMessage(null, ERROR_TYPE_INVALID, bundleEntry.getId(),
                    INVALID_DISPENSE_MEDICATION_REFERENCE_URL, ERROR));
        return new ArrayList<>();
    }


    private List<ValidationMessage> validateMedication(BundleEntryComponent bundleEntry) {
        Property medication = bundleEntry.getResource().getChildByName(MEDICATION);
        if ((medication == null) || (!medication.hasValues())) {
            logger.debug(String.format("Medication-Prescription:Encounter failed for %s", UNSPECIFIED_MEDICATION));
            return validationMessages(new ValidationMessage(null, ERROR_TYPE_INVALID, bundleEntry.getId(),
                    UNSPECIFIED_MEDICATION,
                    ERROR));
        }
        String medicationRefUrl = getReferenceUrl(medication);
        if ((medicationRefUrl == null)) return new ArrayList<>();
        if (!urlValidator.isValid(medicationRefUrl)) {
            logger.debug(String.format("Medication-Prescription:Encounter failed for %s", INVALID_MEDICATION_REFERENCE_URL));
            return validationMessages(new ValidationMessage(null, ERROR_TYPE_INVALID, bundleEntry.getId(),
                    INVALID_MEDICATION_REFERENCE_URL, ERROR));
        }
        if (!isValidCodeableConceptUrl(medicationRefUrl, ""))
            return validationMessages(new ValidationMessage(null, ERROR_TYPE_INVALID, bundleEntry.getId(),
                    INVALID_MEDICATION_REFERENCE_URL, ERROR));
        return new ArrayList<>();
    }

    private List<ValidationMessage> validationMessages(ValidationMessage message) {
        List<ValidationMessage> validationMessages = new ArrayList<>();
        validationMessages.add(message);
        return validationMessages;
    }

    private boolean isValidCodeableConceptUrl(String url, String code) {
        return codeValidator.isValid(url, code).toBlocking().first();
    }

    private String getReferenceUrl(Property medication) {
        Base base = medication.getValues().get(0);
        return base instanceof Reference ? ((Reference) base).getReference() : null;
    }


}
