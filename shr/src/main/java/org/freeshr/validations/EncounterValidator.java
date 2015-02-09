package org.freeshr.validations;


import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.application.fhir.EncounterValidationResponse;
import org.freeshr.application.fhir.FhirMessageFilter;
import org.freeshr.utils.BundleDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.freeshr.application.fhir.EncounterValidationResponse.createErrorResponse;
import static org.freeshr.application.fhir.EncounterValidationResponse.fromValidationMessages;

@Component
public class EncounterValidator {

    private final BundleDeserializer bundleDeserializer;
    private FhirMessageFilter fhirMessageFilter;
    private FhirSchemaValidator fhirSchemaValidator;
    private ResourceValidator resourceValidator;
    private HealthIdValidator healthIdValidator;
    private StructureValidator structureValidator;

    @Autowired
    public EncounterValidator(FhirMessageFilter fhirMessageFilter,
                              FhirSchemaValidator fhirSchemaValidator,
                              ResourceValidator resourceValidator,
                              HealthIdValidator healthIdValidator,
                              StructureValidator structureValidator) {
        this.fhirMessageFilter = fhirMessageFilter;
        this.fhirSchemaValidator = fhirSchemaValidator;
        this.resourceValidator = resourceValidator;
        this.healthIdValidator = healthIdValidator;
        this.structureValidator = structureValidator;
        this.bundleDeserializer = new BundleDeserializer();
    }

    public EncounterValidationResponse validate(EncounterBundle encounterBundle) {
        try {
            final EncounterValidationContext validationContext = new EncounterValidationContext(encounterBundle,
                    bundleDeserializer);

            EncounterValidationResponse validationResponse = fromValidationMessages(fhirSchemaValidator.validate(
                    validationContext.sourceFragment()), fhirMessageFilter);
            if (validationResponse.isNotSuccessful()) return validationResponse;

            validationResponse = fromValidationMessages(structureValidator.validate(validationContext.getBundle())
                    , fhirMessageFilter);
            if (validationResponse.isNotSuccessful()) return validationResponse;

            validationResponse = fromValidationMessages(resourceValidator.validate(validationContext.getBundle()), fhirMessageFilter);
            return validationResponse.isSuccessful() ? fromValidationMessages(healthIdValidator.validate(validationContext), fhirMessageFilter) : validationResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(e);
        }
    }


}
