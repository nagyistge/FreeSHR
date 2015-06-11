package org.freeshr.util;

import org.hl7.fhir.instance.validation.ValidationMessage;

import java.util.List;

import static ch.lambdaj.Lambda.*;
import static org.hamcrest.Matchers.equalTo;

public class ValidationMessageList {
    private List<ValidationMessage> validationMessages;

    public ValidationMessageList(List<ValidationMessage> validationMessages) {
        this.validationMessages = validationMessages;
    }


    public boolean hasMessage(String message){
        return select(validationMessages, having(on(ValidationMessage.class).getMessage(), equalTo(message))) != null;
    }
}
