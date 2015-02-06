package org.freeshr.validations;


import org.freeshr.domain.ErrorMessageBuilder;
import org.hl7.fhir.instance.model.*;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.freeshr.domain.ErrorMessageBuilder.buildValidationMessage;

@Component
public class ProcedureValidator implements Validator<BundleEntryComponent> {

    private static final Logger logger = LoggerFactory.getLogger(ProcedureValidator.class);
    private static String DATE = "date";
    private static String REPORT = "report";
    private DateValidator dateValidator;

    @Autowired
    public ProcedureValidator(DateValidator dateValidator) {
        this.dateValidator = dateValidator;
    }

    @Override
    public List<ValidationMessage> validate(BundleEntryComponent entry) {
        List<ValidationMessage> validationMessages = new ArrayList<>();


        if (!validateDate(entry, validationMessages)) {
            return validationMessages;

        }

        if (!validateDiagnosticReport(entry, validationMessages)) {
            return validationMessages;

        }

        return validationMessages;
    }

    //TODO: Decide if this func should be written(checks if link is present in the entire feed (Now checking for whether the link is not empty)
    private boolean validateDiagnosticReport(BundleEntryComponent entry, List<ValidationMessage> validationMessages) {
        String id = entry.getId();
        Property report = entry.getResource().getChildByName(REPORT);
        List<Base> reportElements = report.getValues();
        for (Base reportElement : reportElements) {
            if (reportElement instanceof Reference) {
                Reference reference = (Reference) reportElement;
                if (reference.getReference() == null || reference.getReference().isEmpty()) {
                    logger.error("Should have reference to Diagnostic Report resource");
                    validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, ErrorMessageBuilder.INVALID_DIAGNOSTIC_REPORT_REFERNECE, OperationOutcome.IssueSeverity.ERROR));
                    return false;
                }
            }
        }


        return true;

    }

    private boolean validateDate(BundleEntryComponent entry, List<ValidationMessage> validationMessages) {
        String id = entry.getId();
        Property date = entry.getResource().getChildByName(DATE);
        List<Base> dateElements = date.getValues();
        for (Base element : dateElements) {
            if (element instanceof Period) {
                Period period = (Period) element;
                Date endDate = period.getEnd();
                Date startDate = period.getStart();


                if (!dateValidator.isValidPeriod(startDate, endDate)) {
                    logger.error("Invalid Period Date. ");
                    validationMessages.add(buildValidationMessage(id, ResourceValidator.INVALID, ErrorMessageBuilder.INVALID_PERIOD, OperationOutcome.IssueSeverity.ERROR));
                    return false;
                }

            }
        }

        return true;
    }
}
