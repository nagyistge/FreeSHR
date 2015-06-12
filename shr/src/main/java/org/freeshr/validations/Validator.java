package org.freeshr.validations;

import org.hl7.fhir.instance.validation.ValidationMessage;

import java.util.List;

public interface Validator<T> {
    String ERROR_TYPE_INVALID = "invalid";
    String ERROR_TYPE_CODE_UNKNOWN = "code-unknown";
    String ERROR_TYPE_STRUCTURE = "structure";

    List<ValidationMessage> validate(T entry);
}
