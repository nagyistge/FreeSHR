package org.freeshr.validations;

import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.utils.BundleDeserializer;
import org.hl7.fhir.instance.model.Bundle;

public class EncounterValidationContext {
    private EncounterBundle encounterBundle;
    private Bundle bundle;

    public EncounterValidationContext(EncounterBundle encounterBundle) {
        this.encounterBundle = encounterBundle;
    }

    public Bundle getBundle() {
        //deserialize only once
        if (bundle != null) return bundle;
        bundle = new BundleDeserializer().deserialize(encounterBundle.getContent());
        return bundle;
    }

    public String getHealthId() {
        return this.encounterBundle.getHealthId();
    }

    public EncounterValidationContext context() {
        return this;
    }

    public String getContent() {
        return encounterBundle.getContent();
    }
}
