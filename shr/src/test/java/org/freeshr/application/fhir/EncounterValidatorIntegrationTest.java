package org.freeshr.application.fhir;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.freeshr.config.SHRConfig;
import org.freeshr.config.SHREnvironmentMock;
import org.freeshr.config.SHRProperties;
import org.freeshr.data.EncounterBundleData;
import org.freeshr.infrastructure.tr.ValueSetCodeValidator;
import org.freeshr.utils.CollectionUtils;
import org.freeshr.utils.FileUtil;
import org.freeshr.validations.*;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.utils.ITerminologyServices;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.List;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.freeshr.domain.ErrorMessageBuilder.INVALID_DISPENSE_MEDICATION_REFERENCE_URL;
import static org.freeshr.domain.ErrorMessageBuilder.INVALID_MEDICATION_REFERENCE_URL;
import static org.freeshr.utils.FileUtil.asString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRConfig.class)
public class EncounterValidatorIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);
    @Autowired
    ValueSetCodeValidator valueSetCodeValidator;
    EncounterBundle encounterBundle;
    private EncounterValidator validator;
    @Autowired
    private TRConceptLocator trConceptLocator;
    @Autowired
    private SHRProperties shrProperties;
    @Autowired
    private ResourceValidator resourceValidator;
    @Autowired
    private HealthIdValidator healthIdValidator;
    @Autowired
    private StructureValidator structureValidator;
    @Autowired
    private FhirMessageFilter fhirMessageFilter;
    private FhirSchemaValidator fhirSchemaValidator;
    @Autowired
    private AsyncRestTemplate asyncRestTemplate;
    @Autowired
    @Qualifier("fhirTrMap")
    private Properties fhirTrMap;

    @Before
    public void setup() throws Exception {
        fhirSchemaValidator = new FhirSchemaValidator(trConceptLocator, shrProperties, fhirTrMap, asyncRestTemplate);
        validator = new EncounterValidator(fhirMessageFilter, fhirSchemaValidator, resourceValidator,
                healthIdValidator, structureValidator);
        encounterBundle = EncounterBundleData.withValidEncounter();

        givenThat(get(urlEqualTo("/openmrs/ws/rest/v1/tr/drugs/3be99d23-e50d-41a6-ad8c-f6434e49f513"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asString("jsons/medication_paracetamol.json"))));

        givenThat(get(urlEqualTo("/openmrs/ws/rest/v1/tr/vs/Quantity-Units"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asString("jsons/code.json"))));

    }

    @Test
    public void shouldValidateEncounterWhenInProperFormat() throws Exception {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/encounter.xml"));
        EncounterValidationResponse validate = validator.validate(encounterBundle);
        assertTrue(validate.isSuccessful());
    }

    @Test
    public void shouldFailIfConditionStatusIsInvalid() throws Exception {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/invalid_condition.xml"));
        EncounterValidationResponse response = validator.validate(encounterBundle);
        assertFalse(response.isSuccessful());
        assertEquals(1, response.getErrors().size());
        assertEquals("Unknown", response.getErrors().get(0).getField());
    }

    @Test
    public void shouldRejectEncounterWithMissingSystemForDiagnosis() throws Exception {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/diagnosis_system_missing.xml"));
        assertFalse(validator.validate(encounterBundle).isSuccessful());
    }

    @Test
    public void shouldRejectEncountersWithDiagnosisHavingAllInvalidSystems() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/diagnosis_system_invalid.xml"));
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        assertFalse(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldTreatFHIRWarningAsError() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/diagnosis_system_invalid.xml"));
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        assertFalse(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldRejectInvalidDiagnosisCategory() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/diagnosis_category_invalid.xml"));
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        assertFalse(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateIfTheHealthIdInTheEncounterContentIsNotSameAsTheOneExpected() {
        encounterBundle.setHealthId("1111222233334444555");
        EncounterValidationResponse response = validator.validate(encounterBundle);
        assertFalse(response.isSuccessful());
        assertThat(response.getErrors().size(), is(2));
        assertTrue(response.getErrors().get(0).getReason().contains("Health Id does not match"));
        assertTrue(response.getErrors().get(1).getReason().contains("Health Id does not match"));
    }

    @Test
    public void shouldValidateInvalidSchemaInDischargeSummaryEncounter() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/discharge_summary_encounter_invalid.xml"));

        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        List<Error> errors = validationResponse.getErrors();
        assertEquals("Unknown", errors.get(0).getField());
        assertFalse(validationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateInvalidMedicationInDischargeSummaryEncounter() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/discharge_summary_encounter_medication_invalid.xml"));

        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertFalse(validationResponse.isSuccessful());
        List<Error> errors = validationResponse.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Invalid Medication Reference URL", errors.get(0).getReason());
    }

    @Test
    public void shouldValidateMissingSystemCodeInDischargeSummaryEncounter() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/discharge_summary_encounter_system_missing.xml"));
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertFalse(validationResponse.isSuccessful());
        assertEquals("Should Fail For Missing System Url", 1, validationResponse.getErrors().size());
    }

    @Test
    public void shouldValidateProcedure() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/procedure/encounter_Procedure.xml"));
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertTrue(validationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateEncounter() throws Exception {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/bundle/registration_encounter.xml"));
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertTrue(validationResponse.isSuccessful());

    }
}
