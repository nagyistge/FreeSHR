package org.freeshr.validations.providerIdentifiers;

import org.freeshr.utils.BundleHelper;
import org.hl7.fhir.instance.model.Resource;
import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MedicationPrescriberIdentifierTest {

    private MedicationPrescriberIdentifier medicationPrescriberIdentifier;

    @Before
    public void setUp() {
        medicationPrescriberIdentifier = new MedicationPrescriberIdentifier();
    }

    @Test
    public void shouldValidateResourceOfTypeMedicationPrescription() {
        Resource resource = BundleHelper.getResource
                ("xmls/encounters/providers_identifiers/medication_prescription.xml",
                        ResourceType.MedicationPrescription);
        assertTrue(medicationPrescriberIdentifier.validates(resource));
    }

    @Test
    public void shouldExtractProperMedicationPrescriptionPerformerReference() {
        Resource resource = BundleHelper.getResource(
                "xmls/encounters/providers_identifiers/medication_prescription.xml",
                ResourceType.MedicationPrescription);
        List<String> references = medicationPrescriberIdentifier.extractUrls(resource);
        assertEquals(1, references.size());
        assertEquals("http://127.0.0.1:9997/providers/18.json", references.get(0));
    }

    @Test
    public void shouldNotValidateResourceOfOtherType() {
        Resource resource = BundleHelper.getResource
                ("xmls/encounters/providers_identifiers/encounter_with_valid_participant.xml",
                ResourceType.Encounter);
        assertFalse(medicationPrescriberIdentifier.validates(resource));
    }

}