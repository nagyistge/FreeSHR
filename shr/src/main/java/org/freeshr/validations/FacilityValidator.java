package org.freeshr.validations;

import org.freeshr.config.SHRProperties;
import org.freeshr.domain.model.Facility;
import org.freeshr.domain.service.FacilityService;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

import static org.freeshr.application.fhir.ValidationErrorType.INVALID;
import static org.hl7.fhir.instance.model.OperationOutcome.IssueSeverity;

@Component
public class FacilityValidator implements Validator<Bundle> {

    public static final String INVALID_SERVICE_PROVIDER = "Invalid Service Provider";
    public static final String INVALID_SERVICE_PROVIDER_URL = "Invalid Service Provider URL";
    private final static Logger logger = LoggerFactory.getLogger(FacilityValidator.class);
    private SHRProperties shrProperties;
    private FacilityService facilityService;

    @Autowired
    public FacilityValidator(SHRProperties shrProperties, FacilityService facilityService) {
        this.shrProperties = shrProperties;
        this.facilityService = facilityService;
    }

    @Override
    public List<ValidationMessage> validate(Bundle bundle) {
        List<ValidationMessage> validationMessages = new ArrayList<>();
        BundleEntryComponent encounterEntry = identifyEncounterEntry(bundle);
        Reference serviceProvider = getServiceProviderRef(encounterEntry);
        if (serviceProvider == null) {
            logger.debug("Validating encounter as facility is not provided");
            return validationMessages;
        }
        String facilityUrl = serviceProvider.getReference();
        if (facilityUrl.isEmpty() || !isValidFacilityUrl(facilityUrl)) {
            validationMessages.add(buildValidationMessage(INVALID, encounterEntry.getId(),
                    INVALID_SERVICE_PROVIDER_URL, IssueSeverity.ERROR));
            logger.debug("Encounter failed for invalid facility URL");
            return validationMessages;
        }

        Facility facility = checkForFacility(facilityUrl).toBlocking().first();
        if (facility == null) {
            validationMessages.add(buildValidationMessage(INVALID, encounterEntry.getId(), INVALID_SERVICE_PROVIDER,
                    IssueSeverity.ERROR));
            return validationMessages;
        }

        logger.debug(String.format("Encounter validated for valid facility %s", facility.getFacilityId()));
        return validationMessages;
    }

    private Reference getServiceProviderRef(BundleEntryComponent encounterEntry) {
        return (encounterEntry != null) ? ((Encounter) encounterEntry.getResource()).getServiceProvider() : null;
    }

    private BundleEntryComponent identifyEncounterEntry(Bundle bundle) {
        for (BundleEntryComponent bundleEntry : bundle.getEntry()) {
            Resource resource = bundleEntry.getResource();
            if (resource instanceof Encounter) {
                return bundleEntry;
            }
        }
        return null;
    }

    private ValidationMessage buildValidationMessage(String type, String path, String message, IssueSeverity error) {
        return new ValidationMessage(ValidationMessage.Source.ResourceValidator, type, path, message, error);
    }

    private Observable<Facility> checkForFacility(String facilityUrl) {
        Observable<Facility> facilityObservable = facilityService.ensurePresent(extractFacilityId(facilityUrl));
        return facilityObservable.flatMap(new Func1<Facility, Observable<Facility>>() {
                                              @Override
                                              public Observable<Facility> call(Facility facility) {
                                                  return Observable.just(facility);
                                              }
                                          },
                new Func1<Throwable, Observable<Facility>>() {
                    @Override
                    public Observable<Facility> call(Throwable throwable) {
                        logger.debug("Facility not found");
                        return Observable.just(null);
                    }
                },
                new Func0<Observable<Facility>>() {
                    @Override
                    public Observable<Facility> call() {
                        return null;
                    }
                });
    }

    private String extractFacilityId(String referenceSimple) {
        return referenceSimple.substring(referenceSimple.lastIndexOf('/') + 1, referenceSimple.lastIndexOf('.')).trim();
    }

    private boolean isValidFacilityUrl(String referenceSimple) {
        String facilityRegistryUrl = shrProperties.getFacilityReferencePath();
        return referenceSimple.contains(facilityRegistryUrl);
    }
}