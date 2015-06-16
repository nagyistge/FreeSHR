package org.freeshr.validations.providerIdentifiers;

import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.freeshr.utils.BundleHelper.getResource;
import static org.junit.Assert.*;

public class ProcedurePerformerIdentifierTest {

    private ProcedurePerformerIdentifier procedurePerformerIdentifier;

    @Before
    public void setUp() {
        procedurePerformerIdentifier = new ProcedurePerformerIdentifier();
    }

    @Test
    public void shouldValidateResourceOfTypeProcedure() {
        assertTrue(procedurePerformerIdentifier.validates(getResource("xmls/encounters/providers_identifiers/procedure.xml", ResourceType
                .Procedure)));
    }

    @Test
    public void shouldExtractProperProcedurePerformerReferences() {
        List<String> references = procedurePerformerIdentifier.extractUrls(getResource("xmls/encounters/providers_identifiers/procedure" +
                ".xml", ResourceType.Procedure));
        assertEquals(1, references.size());
        assertEquals("http://127.0.0.1:9997/providers/18.json", references.get(0));
    }

    @Test
    public void shouldNotValidateResourceOfOtherType() {
        assertFalse(procedurePerformerIdentifier.validates(getResource
                ("xmls/encounters/providers_identifiers/encounter_with_valid_participant.xml", ResourceType.Encounter)));
    }
}