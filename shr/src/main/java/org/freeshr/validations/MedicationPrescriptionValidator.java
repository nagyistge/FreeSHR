package org.freeshr.validations;


import org.freeshr.domain.ErrorMessageBuilder;
import org.freeshr.infrastructure.tr.MedicationCodeValidator;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.utils.ITerminologyServices;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

import static org.freeshr.domain.ErrorMessageBuilder.*;
import static org.hl7.fhir.instance.model.OperationOutcome.IssueSeverity;

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
    public List<ValidationMessage> validate(BundleEntryComponent entry) {

        ArrayList<ValidationMessage> validationMessages = new ArrayList<>();

        if (!validateMedication(entry, validationMessages)) {
            return validationMessages;
        }

        if (!validateDosageQuantity(entry, validationMessages)) {
            return validationMessages;
        }

        if (!validateDispenseMedication(entry, validationMessages)) {
            return validationMessages;
        }

        return validationMessages;
    }

    private boolean validateDosageQuantity(BundleEntryComponent entry, ArrayList<ValidationMessage> validationMessages) {
        String id = entry.getId();
        Property dosageInstruction = entry.getResource().getChildByName(DOSAGE_INSTRUCTION);
        List<Base> dosageInstructionValues = dosageInstruction.getValues();
        for (Base dosageInstructionValue : dosageInstructionValues) {

            if (dosageInstructionValue instanceof MedicationPrescription.MedicationPrescriptionDosageInstructionComponent) {
                Quantity doseQuantity = ((MedicationPrescription.MedicationPrescriptionDosageInstructionComponent) dosageInstructionValue).getDoseQuantity();

                if(doseQuantityValidator.isReferenceUrlNotFound(doseQuantity)){
                    return true;
                }

                if(!urlValidator.isValid(doseQuantity.getSystem())){
                    return false;
                }

                ITerminologyServices.ValidationResult validationResult = doseQuantityValidator.validate(doseQuantity);
                if (validationResult != null) {
                    logger.error("Medication-Prescription DosageQuantity Code is invalid:");
                    validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, ErrorMessageBuilder.INVALID_DOSAGE_QUANTITY, IssueSeverity.ERROR));
                    return false;
                }
            }
        }

        return true;
    }


    private boolean validateDispenseMedication(BundleEntryComponent entry, ArrayList<ValidationMessage> validationMessages) {

        String id = entry.getId();
        Property dispense = entry.getResource().getChildByName(DISPENSE);
        /* Not a Mandatory Field.Skip it if not present */
        if (dispense == null || (!dispense.hasValues())) {
            return true;
        }
        Property dispenseMedication = dispense.getValues().get(0).getChildByName(MEDICATION);
        if (dispenseMedication == null || !dispenseMedication.hasValues()) {
            return true;
        }
        String dispenseMedicationRefUrl = getReferenceUrl(dispenseMedication);
        if ((dispenseMedicationRefUrl == null)) {
            return true;
        }
        if ((!urlValidator.isValid(dispenseMedicationRefUrl))) {
            logger.error("Dispense-Medication URL is invalid:" + dispenseMedicationRefUrl);
            validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, INVALID_DISPENSE_MEDICATION_REFERENCE_URL, IssueSeverity.ERROR));
            return false;
        }

        if (!isValidCodeableConceptUrl(dispenseMedicationRefUrl, "")) {
            validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, INVALID_DISPENSE_MEDICATION_REFERENCE_URL, IssueSeverity.ERROR));
            return false;
        }

        return true;
    }


    private boolean validateMedication(BundleEntryComponent entry, List<ValidationMessage> validationMessages) {
        String id = entry.getId();
        Property medication = entry.getResource().getChildByName(MEDICATION);
        if ((medication == null) || (!medication.hasValues())) {
            validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, UNSPECIFIED_MEDICATION, IssueSeverity.ERROR));
            return false;
        }

        String medicationRefUrl = getReferenceUrl(medication);
        //now to check for valid or invalid
        if ((medicationRefUrl == null)) {
            return true;
        }
        if ((!urlValidator.isValid(medicationRefUrl))) {
            logger.error("Medication URL is invalid:" + medicationRefUrl);
            validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, INVALID_MEDICATION_REFERENCE_URL, IssueSeverity.ERROR));
            return false;
        }

        if (!isValidCodeableConceptUrl(medicationRefUrl, "")) {
            validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, INVALID_MEDICATION_REFERENCE_URL, IssueSeverity.ERROR));
            return false;
        }

        return true;
    }

    private boolean isValidCodeableConceptUrl(String url, String code) {

        Observable<Boolean> obs = codeValidator.isValid(url, code);
        Boolean result = obs.toBlocking().first();
        if (!result) {
            return false;
        }
        return true;
    }


    private String getReferenceUrl(Property medication) {
        Base element = medication.getValues().get(0);
        if (element instanceof Reference) {
            return ((Reference) element).getReference();
        }
        return null;
    }



}
