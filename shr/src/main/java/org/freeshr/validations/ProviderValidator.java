package org.freeshr.validations;

import org.freeshr.config.SHRProperties;
import org.freeshr.validations.providerIdentifiers.ClinicalResourceProviderIdentifier;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.freeshr.validations.ResourceValidator.INVALID;
import static org.hl7.fhir.instance.model.OperationOutcome.IssueSeverity.ERROR;

@Component
public class ProviderValidator implements Validator<Bundle> {

    private static final Logger logger = LoggerFactory.getLogger(ProviderValidator.class);
    private List<ClinicalResourceProviderIdentifier> clinicalResourceProviderIdentifiers;
    private SHRProperties shrProperties;

    @Autowired
    public ProviderValidator(List<ClinicalResourceProviderIdentifier> clinicalResourceProviderIdentifiers,
                             SHRProperties shrProperties) {
        this.clinicalResourceProviderIdentifiers = clinicalResourceProviderIdentifiers;
        this.shrProperties = shrProperties;
    }

    @Override
    public List<ValidationMessage> validate(Bundle bundle) {
        List<BundleEntryComponent> entryList = bundle.getEntry();
        List<ValidationMessage> validationMessages = new ArrayList<>();
        for (BundleEntryComponent entry : entryList) {
            Resource resource = entry.getResource();
            for (ClinicalResourceProviderIdentifier clinicalResourceProviderIdentifier : clinicalResourceProviderIdentifiers) {
                if (!clinicalResourceProviderIdentifier.isValid(resource, shrProperties)) {
                    logger.debug(String.format("Provider:Encounter failed for %s", ValidationMessages.INVALID_PROVIDER_URL));
                    validationMessages.add(new ValidationMessage(ValidationMessage.Source.ProfileValidator,
                            INVALID, entry.getId(),
                            ValidationMessages.INVALID_PROVIDER_URL + " in " + resource.getResourceType().getPath(), ERROR));
                }
            }
        }
        return validationMessages;
    }
}

