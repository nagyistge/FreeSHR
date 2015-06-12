package org.freeshr.application.fhir;

import org.freeshr.utils.CollectionUtils;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FhirMessageFilter {

    private ArrayList<String> ignoreList;

    public FhirMessageFilter() {
        ignoreList = new ArrayList<>();
        ignoreList.add("f:DiagnosticOrder/f:item");
        ignoreList.add("f:DiagnosticReport/f:name");
    }

    public EncounterValidationResponse getValidationResponseFromMessagesSevereThan(List<ValidationMessage> outputs,
                                                                                   final OperationOutcome.IssueSeverity severity) {
        List<ValidationMessage> extractedMessages = extractMessagesSevereThan(outputs, severity);

        return CollectionUtils.reduce(extractedMessages, new EncounterValidationResponse(), new CollectionUtils.ReduceFn<ValidationMessage,
                EncounterValidationResponse>() {
            @Override
            public EncounterValidationResponse call(ValidationMessage input, EncounterValidationResponse acc) {
                Error error = new Error();
                error.setField(input.getLocation());
                error.setType(input.getType());
                error.setReason(input.getMessage());
                acc.addError(error);
                return acc;
            }
        });
    }

    public List<ValidationMessage> extractMessagesSevereThan(List<ValidationMessage> outputs, final OperationOutcome.IssueSeverity severity) {
        return CollectionUtils.filter(outputs, new CollectionUtils.Fn<ValidationMessage,
                Boolean>() {
            @Override
            public Boolean call(ValidationMessage input) {
                //For SHR: We treat FHIR warning level as error.
                boolean possibleError = severity.compareTo(input.getLevel()) >= 0;
                // TODO :  remove the following if condition once the validation mechanism is finalised for
                // DiagnosticOrder
                if (possibleError) {
                    if (shouldFilterMessagesOfType(input)) {
                        possibleError = false;
                    }
                }
                return possibleError;

            }
        });
    }

    private boolean shouldFilterMessagesOfType(ValidationMessage input) {
        if (input.getLevel().equals(OperationOutcome.IssueSeverity.ERROR))
            return false;
        if (input.getType().equalsIgnoreCase("code-unknown")) {
            for (String ignoreString : ignoreList) {
                if (input.getLocation().contains(ignoreString)) {
                    return true;
                }
            }
        }
        return false;
    }
}