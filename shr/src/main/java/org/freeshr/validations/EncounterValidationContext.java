package org.freeshr.validations;

import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.utils.ResourceOrFeedDeserializer;
import org.hl7.fhir.instance.model.Bundle;

public class EncounterValidationContext {
    private EncounterBundle encounterBundle;
    private ResourceOrFeedDeserializer resourceOrFeedDeserializer;
    private Bundle feed;

    public EncounterValidationContext(EncounterBundle encounterBundle,
                                      ResourceOrFeedDeserializer resourceOrFeedDeserializer) {
        this.encounterBundle = encounterBundle;
        this.resourceOrFeedDeserializer = resourceOrFeedDeserializer;
    }

    public Bundle getBundle() {
        //deserialize only once
        if (feed != null) return feed;
        feed = resourceOrFeedDeserializer.deserialize(encounterBundle.getContent());
        return feed;
    }

    public String getHealthId() {
        return this.encounterBundle.getHealthId();
    }

    public String sourceFragment() {
        return encounterBundle.getContent();
    }
}
