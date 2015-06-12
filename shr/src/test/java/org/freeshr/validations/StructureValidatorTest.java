package org.freeshr.validations;


import org.freeshr.application.fhir.ValidationErrorType;
import org.freeshr.util.ValidationMessageList;
import org.freeshr.utils.BundleDeserializer;
import org.freeshr.utils.FileUtil;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StructureValidatorTest {

    private BundleDeserializer bundleDeserializer;
    private StructureValidator structureValidator;

    @Before
    public void setup() {
        bundleDeserializer = new BundleDeserializer();
        structureValidator = new StructureValidator();
    }

    @Test
    public void shouldAcceptAValidXmlWithOneEntryForEachSectionPresentInComposition() {
        final String xml = FileUtil.asString("xmls/encounters/diagnostic_order_valid.xml");
        List<ValidationMessage> validationMessages = structureValidator.validate(bundleDeserializer.deserialize(xml));
        ValidationMessageList messageList = new ValidationMessageList(validationMessages);
        assertTrue(messageList.isEmpty());
    }

    @Test
    public void shouldRejectIfCompositionHasNoEncounterPresent() {
        final String xml = FileUtil.asString("xmls/encounters/composition_with_no_encounter.xml");

        List<ValidationMessage> validationMessages = structureValidator.validate(bundleDeserializer.deserialize(xml));
        ValidationMessageList messageList = new ValidationMessageList(validationMessages);
        assertFalse(messageList.isEmpty());
        assertTrue(messageList.isOfSize(1));
        assertTrue(messageList.hasErrorOfTypeAndMessage("Feed must have a Composition with an encounter.", ValidationErrorType.STRUCTURE));
    }

    @Test
    public void shouldRejectIfCompositionDoesNotContainASectionCalledEncounter() {
        final String xml = FileUtil.asString("xmls/encounters/invalid_composition.xml");
        List<ValidationMessage> validationMessages = structureValidator.validate(bundleDeserializer.deserialize(xml));
        assertThat(validationMessages.isEmpty(), is(false));
        assertThat(validationMessages.size(), is(1));
        assertThat(validationMessages.get(0).getMessage(), is("Feed must have a Composition with an encounter."));
    }

    @Test
    public void shouldRejectIfThereIsAMismatchBetweenEntriesAndSections() {
        /*
         Scenarios Covered

        1. No entry present for the section
        2. Ids mismatching in the entry (2 errors for this)
        3. An entry with no matching section

         */
        final String xml = FileUtil.asString("xmls/encounters/invalid_composition_sections.xml");
        List<ValidationMessage> validationMessages = structureValidator.validate(bundleDeserializer.deserialize(xml));
        assertThat(validationMessages.isEmpty(), is(false));
        assertThat(validationMessages.size(), is(4));
    }
}