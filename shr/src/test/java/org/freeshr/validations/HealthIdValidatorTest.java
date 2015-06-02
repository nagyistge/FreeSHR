package org.freeshr.validations;

import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.application.fhir.EncounterValidationResponse;
import org.freeshr.application.fhir.FhirMessageFilter;
import org.freeshr.config.SHRProperties;
import org.freeshr.utils.BundleDeserializer;
import org.freeshr.utils.FileUtil;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class HealthIdValidatorTest {

    private HealthIdValidator healthIdValidator;
    BundleDeserializer bundleDeserializer;
    FhirMessageFilter fhirMessageFilter;

    @Mock
    SHRProperties shrProperties;

    @Before
    public void setup() {
        initMocks(this);

        healthIdValidator = new HealthIdValidator(shrProperties);
        bundleDeserializer = new BundleDeserializer();
        fhirMessageFilter = new FhirMessageFilter();
    }

    @Test
    public void shouldAcceptEncounterIfHealthIdInTheXmlMatchesTheGivenHealthId() {
        final String xml = FileUtil.asString("xmls/encounters/diagnostic_order_valid.xml");
        Bundle bundle = bundleDeserializer.deserialize(xml);
        when(shrProperties.getPatientReferencePath()).thenReturn("http://localhost:9997/api/default/patients");
        List<ValidationMessage> response = healthIdValidator.validate(getEncounterContext(xml, "5893922485019082753"));
        assertThat(EncounterValidationResponse.fromValidationMessages(response, fhirMessageFilter).isSuccessful(),
                is(true));
    }


    @Test
    public void shouldAcceptEncounterIfHealthIdInTheXmlMatchesTheGivenHealthIdAllVersions() {
        final String xml = FileUtil.asString("xmls/encounters/diagnostic_order_valid.xml");
        AtomFeed feed = resourceOrFeedDeserializer.deserialize(xml);
        when(shrProperties.getPatientReferencePath()).thenReturn("http://localhost:9997/api/v1/patients");
        List<ValidationMessage> response = healthIdValidator.validate(getEncounterContext(xml, "5893922485019082753"));
        assertThat(EncounterValidationResponse.fromValidationMessages(response, fhirMessageFilter).isSuccessful(),
                is(true));
    }

    @Test
    public void shouldNotAcceptEncounterIfNoHealthIdIsPresentInComposition() {
        String xml = FileUtil.asString("xmls/encounters/invalid_composition.xml");
        Bundle bundle = bundleDeserializer.deserialize(xml);
        EncounterValidationResponse response = EncounterValidationResponse.fromValidationMessages(
                healthIdValidator.validate(getEncounterContext(xml, "5893922485019082753")), fhirMessageFilter);
        assertThat(response.isSuccessful(), is(false));
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().get(0).getReason(), is("Composition must have patient's Health Id in subject."));
    }

    @Test
    public void shouldRejectEncounterIfHealthIdInTheXmlDoesNotMatchTheGivenHealthId() {
        when(shrProperties.getPatientReferencePath()).thenReturn("http://172.18.46.56:8081/api/default/patients");
        String xml = FileUtil.asString("xmls/encounters/encounter.xml");
        Bundle bundle = bundleDeserializer.deserialize(xml);
        EncounterValidationResponse response = EncounterValidationResponse.fromValidationMessages(
                healthIdValidator.validate(getEncounterContext(xml, "11112222233333")), fhirMessageFilter);
        assertThat(response.isSuccessful(), is(false));
        assertThat(response.getErrors().get(0).getType(), is(ResourceValidator.INVALID));
        assertThat(response.getErrors().get(0).getReason(), is("Patient's Health Id does not match."));
    }

    @Test
    public void shouldRejectEncounterIfThereIsNoHealthIdInTheComposition() {
        when(shrProperties.getPatientReferencePath()).thenReturn("http://172.18.46.56:8081/api/default/patients");
        String xml = FileUtil.asString("xmls/encounters/encounter.xml");
        Bundle bundle = bundleDeserializer.deserialize(xml);
        EncounterValidationResponse response = EncounterValidationResponse.fromValidationMessages(
                healthIdValidator.validate(getEncounterContext(xml, "11112222233333")), fhirMessageFilter);
        assertThat(response.isSuccessful(), is(false));
        assertThat(response.getErrors().get(0).getType(), is(ResourceValidator.INVALID));
        assertThat(response.getErrors().get(0).getReason(), is("Patient's Health Id does not match."));
    }

    private EncounterValidationContext getEncounterContext(final String xml, final String healthId) {
        EncounterBundle encounterBundle = new EncounterBundle();
        encounterBundle.setEncounterContent(xml);
        encounterBundle.setHealthId(healthId);
        return new EncounterValidationContext(encounterBundle, bundleDeserializer);
    }
}