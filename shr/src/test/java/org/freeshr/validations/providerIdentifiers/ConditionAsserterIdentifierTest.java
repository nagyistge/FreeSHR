package org.freeshr.validations.providerIdentifiers;


import org.hl7.fhir.instance.model.ResourceType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.freeshr.utils.BundleHelper.getResource;
import static org.junit.Assert.*;

public class ConditionAsserterIdentifierTest {

    private ConditionAsserterIdentifier conditionAsserterIdentifier;

    @Before
    public void setUp() {
        conditionAsserterIdentifier = new ConditionAsserterIdentifier();
    }

    @Test
    public void shouldValidateResourceOfTypeCondition() {
        assertTrue(conditionAsserterIdentifier.validates(getResource("xmls/encounters/providers_identifiers/condition.xml", ResourceType
                .Condition)));
    }

    @Test
    public void shouldExtractProperConditionAsserterReferences() {
        List<String> references = conditionAsserterIdentifier.extractUrls(getResource("xmls/encounters/providers_identifiers/condition.xml",
                ResourceType.Condition));
        assertEquals(1, references.size());
        assertEquals("http://127.0.0.1:9997/providers/18.json", references.get(0));

        references = conditionAsserterIdentifier.extractUrls(getResource("xmls/encounters/providers_identifiers/condition_no_asserter" +
                ".xml", ResourceType.Condition));
        assertNull(references);

    }

    @Test
    public void shouldNotValidateResourceOfOtherType() {
        assertFalse(conditionAsserterIdentifier.validates(getResource
                ("xmls/encounters/providers_identifiers/encounter_with_valid_participant.xml",
                ResourceType.Encounter)));
    }
}