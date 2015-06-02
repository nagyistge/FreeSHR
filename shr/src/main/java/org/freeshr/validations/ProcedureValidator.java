package org.freeshr.validations;


import org.hl7.fhir.instance.model.Base;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.model.Period;
import org.hl7.fhir.instance.model.Property;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.freeshr.utils.DateUtil.isValidPeriod;

@Component
public class ProcedureValidator implements Validator<BundleEntryComponent> {

    private static final Logger logger = LoggerFactory.getLogger(ProcedureValidator.class);

    @Override
    public List<ValidationMessage> validate(BundleEntryComponent bundleEntry) {
        List<ValidationMessage> validationMessages = new ArrayList<>();

        validationMessages.addAll(validateDate(bundleEntry));
        if (validationMessages.size() > 0) return validationMessages;

        validationMessages.addAll(validateDiagnosticReport(bundleEntry));
        return validationMessages;
    }

    private List<ValidationMessage> validateDiagnosticReport(BundleEntryComponent atomEntry) {
        Property report = atomEntry.getResource().getChildByName("report");
        List<Base> reportElements = report.getValues();
        for (Base reportElement : reportElements) {
            if (!(reportElement instanceof Reference)) continue;
            Reference reference = (Reference) reportElement;
            if (reference.getReference() == null || reference.getReference().isEmpty()) {
                logger.debug(String.format("Procedure:Encounter failed for %s", ValidationMessages.INVALID_DIAGNOSTIC_REPORT_REFERENCE));
                return validationMessages(new ValidationMessage(null, ResourceValidator.INVALID, atomEntry.getId(), ValidationMessages
                        .INVALID_DIAGNOSTIC_REPORT_REFERENCE, OperationOutcome.IssueSeverity.ERROR));
            }
        }
        return new ArrayList<>();

    }

    private List<ValidationMessage> validateDate(BundleEntryComponent atomEntry) {
        Property date = atomEntry.getResource().getChildByName("date");
        for (Base element : date.getValues()) {
            if (!(element instanceof Period)) continue;

            Period period = (Period) element;
            if (!isValidPeriod(period.getStart(), period.getEnd())) {
                logger.debug(String.format("Procedure:Encounter failed for %s", ValidationMessages.INVALID_PERIOD));
                return validationMessages(new ValidationMessage(null, ResourceValidator.INVALID, atomEntry.getId(), ValidationMessages
                        .INVALID_PERIOD, OperationOutcome.IssueSeverity.ERROR));
            }
        }

        return new ArrayList<>();
    }

    private List<ValidationMessage> validationMessages(ValidationMessage message) {
        List<ValidationMessage> validationMessages = new ArrayList<>();
        validationMessages.add(message);
        return validationMessages;
    }
}
