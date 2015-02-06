package org.freeshr.utils;

import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.data.EncounterBundleData;
import org.freeshr.validations.EncounterValidationContext;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.ResourceType;

import java.util.List;

public class BundleHelper {

    public static Bundle.BundleEntryComponent getBundleEntry(String feedFile, ResourceType resourceType) {
        EncounterBundle encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString(feedFile));
        final EncounterValidationContext validationContext = new EncounterValidationContext(encounterBundle,
                new ResourceOrFeedDeserializer());

        Bundle feed = validationContext.getBundle();
        List<Bundle.BundleEntryComponent> entries = feed.getEntry();
        for (Bundle.BundleEntryComponent entry : entries) {
             if (entry.getResource().getResourceType().equals(resourceType)) {
                 return entry;
             }
        }
        return null;
    }

}
