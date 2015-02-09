package org.freeshr.validations;

import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.utils.BundleDeserializer;
import org.hl7.fhir.instance.model.Bundle;

public class EncounterValidationContext {
    private EncounterBundle encounterBundle;
    private BundleDeserializer bundleDeserializer;
    private Bundle feed;

    public EncounterValidationContext(EncounterBundle encounterBundle,
                                      BundleDeserializer bundleDeserializer) {
        this.encounterBundle = encounterBundle;
        this.bundleDeserializer = bundleDeserializer;
    }

    public Bundle getBundle() {
        //deserialize only once
        if (feed != null) return feed;
        feed = bundleDeserializer.deserialize(encounterBundle.getContent());
        return feed;
    }

    public String getHealthId() {
        return this.encounterBundle.getHealthId();
    }

    public String sourceFragment() {
        return encounterBundle.getContent();
    }
}
