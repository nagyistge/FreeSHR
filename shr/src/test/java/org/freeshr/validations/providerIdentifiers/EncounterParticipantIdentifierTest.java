package org.freeshr.validations.providerIdentifiers;

import org.freeshr.utils.BundleHelper;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EncounterParticipantIdentifierTest {

    private EncounterParticipantIdentifier encounterParticipantIdentifier;

    @Before
    public void setUp() {
        encounterParticipantIdentifier = new EncounterParticipantIdentifier();
    }

    @Test
    public void shouldValidateResourceOfTypeEncounter() {
        BundleEntryComponent bundleEntry = BundleHelper.getBundleEntry
                ("xmls/encounters/providers_identifiers/encounter_with_valid_participant.xml",
                        ResourceType.Encounter);
        assertTrue(encounterParticipantIdentifier.validates(bundleEntry.getResource()));
    }

    @Test
    public void shouldExtractProperEncounterParticipantReferences() {
        BundleEntryComponent bundleEntry = BundleHelper.getBundleEntry
                ("xmls/encounters/providers_identifiers/encounter_with_valid_participant.xml",
                        ResourceType.Encounter);
        List<String> references = encounterParticipantIdentifier.extractUrls(bundleEntry.getResource());
        assertTrue(!CollectionUtils.isEmpty(references));
        assertEquals("http://127.0.0.1:9997/providers/18.json", references.get(0));

    }

}