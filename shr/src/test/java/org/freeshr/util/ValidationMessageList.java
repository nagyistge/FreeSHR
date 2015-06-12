package org.freeshr.util;

import org.freeshr.application.fhir.FhirMessageFilter;
import org.hl7.fhir.instance.validation.ValidationMessage;

import java.util.List;

import static ch.lambdaj.Lambda.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hl7.fhir.instance.model.OperationOutcome.IssueSeverity.ERROR;

public class ValidationMessageList {
    private List<ValidationMessage> validationMessages;

    public ValidationMessageList(List<ValidationMessage> validationMessages) {
        this.validationMessages = new FhirMessageFilter().extractMessagesSevereThan(validationMessages, ERROR);
    }

    public boolean hasMessage(String message){
        return !select(validationMessages, having(on(ValidationMessage.class).getMessage(), equalTo(message))).isEmpty();
    }

    public boolean hasErrorOfTypeAndMessage(String message, String type){
        for (ValidationMessage validationMessage : validationMessages) {
            if(validationMessage.getMessage().equals(message) && validationMessage.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSuccessfull(){
        return this.validationMessages.isEmpty();
    }

    public boolean isOfSize(int size){
        return this.validationMessages.size() == size;
    }

}
