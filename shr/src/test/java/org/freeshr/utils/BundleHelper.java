package org.freeshr.utils;

import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.data.EncounterBundleData;
import org.freeshr.validations.EncounterValidationContext;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;

import java.util.List;

public class BundleHelper {

    public static BundleEntryComponent getBundleEntry(String bundleFile, ResourceType resourceType) {
        EncounterBundle encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString(bundleFile));
        final EncounterValidationContext validationContext = new EncounterValidationContext(encounterBundle
        );

        Bundle bundle = validationContext.getBundle();
        List<BundleEntryComponent> entries = bundle.getEntry();
        for (BundleEntryComponent entry : entries) {
            if (entry.getResource().getResourceType().equals(resourceType)) {
                return entry;
            }
        }
        return null;
    }

    public static Resource getResource(String bundleFile, ResourceType resourceType) {
        return getBundleEntry(bundleFile, resourceType).getResource();
    }

    public static Bundle getBundle(String filePath, String healthId) {
        EncounterBundle encounterBundle = EncounterBundleData.encounter(healthId,
                FileUtil.asString(filePath));
        BundleDeserializer bundleDeserializer = new BundleDeserializer();
        return bundleDeserializer.deserialize(encounterBundle.getContent());
    }
}
