package org.freeshr.application.fhir;

import org.freeshr.config.SHRConfig;
import org.freeshr.config.SHREnvironmentMock;
import org.freeshr.config.SHRProperties;
import org.freeshr.data.EncounterBundleData;
import org.freeshr.utils.CollectionUtils;
import org.freeshr.utils.FileUtil;
import org.freeshr.validations.*;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.utils.ITerminologyServices;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.List;
import java.util.Properties;

import static org.freeshr.domain.ErrorMessageBuilder.INVALID_DISPENSE_MEDICATION_REFERENCE_URL;
import static org.freeshr.domain.ErrorMessageBuilder.INVALID_MEDICATION_REFERENCE_URL;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRConfig.class)
public class EncounterValidatorTest {
    @Mock
    private TRConceptLocator trConceptLocator;
    @Mock
    private AsyncRestTemplate asyncRestTemplate;
    @Mock
    private ResourceValidator resourceValidator;
    @Mock
    private HealthIdValidator healthIdValidator;
    @Mock
    private StructureValidator structureValidator;

    @Autowired
    private Properties fhirTrMap;
    @Autowired
    private FhirMessageFilter fhirMessageFilter;
    @Autowired
    private SHRProperties shrProperties;

    private EncounterBundle encounterBundle;
    private FhirSchemaValidator fhirSchemaValidator;
    private EncounterValidator validator;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        fhirSchemaValidator = new FhirSchemaValidator(trConceptLocator, shrProperties, fhirTrMap, asyncRestTemplate);
        validator = new EncounterValidator(fhirMessageFilter, fhirSchemaValidator, resourceValidator,
                healthIdValidator, structureValidator);
        encounterBundle = EncounterBundleData.withValidEncounter();
    }

    @Test
    public void shouldValidateConditionsToCheckIfCategoriesOtherThanChiefComplaintAreCoded() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/coded_and_noncoded_diagnosis.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(8)).verifiesSystem(anyString());
        assertFalse(encounterValidationResponse.isSuccessful());
        assertThat(encounterValidationResponse.getErrors().size(), is(3));
    }

    @Test
    public void shouldRejectEncounterWithInvalidConceptReferenceTerms() {
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        when(trConceptLocator.validateCode(anyString(), eq("INVALID_REFERENCE_TERM"),
                anyString())).thenReturn(new ITerminologyServices.ValidationResult(OperationOutcome.IssueSeverity.ERROR,
                "Invalid code"));

        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/invalid_ref.xml"));
        assertFalse(validator.validate(encounterBundle).isSuccessful());
    }

    @Test
    public void shouldInvalidateWrongCodesInObservations() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/encounter_with_obs_invalid.xml"));
        when(trConceptLocator.validateCode(anyString(), eq("77405a73-b915-4a93-87a7-f29fe6697fb4-INVALID"),
                anyString())).thenReturn(new ITerminologyServices.ValidationResult(OperationOutcome.IssueSeverity.ERROR,
                "Invalid code"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        List<Error> errors = encounterValidationResponse.getErrors();
        assertEquals(2, errors.size());
        assertEquals("Invalid code", errors.get(0).getReason());
        assertFalse(encounterValidationResponse.isSuccessful());

    }

    @Test
    public void shouldValidateDiagnosticOrder() throws Exception {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/diagnostic_order_valid.xml"));
        String system = "http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/concepts/79647ed4-a60e-4cf5-ba68-cf4d55956cba";
        when(trConceptLocator.verifiesSystem(system)).thenReturn(true);
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(1)).verifiesSystem(system);
        assertTrue(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateSpecimenWithDiagnosticOrder() throws Exception {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID, FileUtil.asString("xmls/encounters/diagnostic_order_with_specimen.xml"));
        String system = "http://172.18.46.53:9080/openmrs/ws/rest/v1/tr/concepts/627b6c75-24ea-40ae-b8b4-e9a95017a25e";
        when(trConceptLocator.verifiesSystem(system)).thenReturn(true);
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(2)).verifiesSystem(system);

        assertTrue(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateDiagnosticReport() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/diagnostic_report.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(4)).verifiesSystem(anyString());
        assertTrue(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateCodesInObservations() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/encounter_with_obs_valid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        assertTrue(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateEncounterTypeAgainstValueSet() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/encounter_with_valid_type.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(1)).verifiesSystem
                ("http://localhost:9997/openmrs/ws/rest/v1/tr/concepts/79647ed4-a60e-4cf5-ba68-cf4d55956cba");
        verify(trConceptLocator, times(1)).verifiesSystem
                ("http://localhost:9997/openmrs/ws/rest/v1/tr/vs/encounter-type");
        verify(trConceptLocator, times(1)).validateCode("http://localhost:9997/openmrs/ws/rest/v1/tr/vs/encounter-type",
                "REG", "registration");
        assertTrue(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateMedicationPrescription() throws Exception {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertTrue(validationResponse.isSuccessful());
    }

    @Test
    public void shouldValidatePrescriptionWithInvalidMedicationReference() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_invalid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);


        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertFalse("Invalid medication prescription should have failed validation", validationResponse.isSuccessful());
        List<Error> invalidUrlError = CollectionUtils.filter(validationResponse.getErrors(), new CollectionUtils.Fn<Error, Boolean>() {
            @Override
            public Boolean call(Error e) {
                return e.getReason().equals(INVALID_MEDICATION_REFERENCE_URL);
            }
        });
        assertEquals("Should have found one invalid medication url", 2, invalidUrlError.size());

    }

    @Test
    public void shouldValidatePrescriptionWithValidMedicationReference() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_valid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(1)).validateCode("http://localhost:9997/openmrs/ws/rest/v1/tr/vs/Route-of-Administration", "implant", "implant");
        assertTrue("Medication prescription pass through validation", validationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateRouteInMedicationPrescription() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_valid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);

        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(1)).verifiesSystem("http://localhost:9997/openmrs/ws/rest/v1/tr/vs/Route-of-Administration");
        verify(trConceptLocator, times(1)).validateCode("http://localhost:9997/openmrs/ws/rest/v1/tr/vs/Route-of-Administration", "implant", "implant");
        assertTrue(encounterValidationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateSiteAndReasonInMedicationPrescription() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_route_valid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(1)).validateCode("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/vs/dosageInstruction-site", "181220002", "Entire oral cavity");
        verify(trConceptLocator, times(1)).validateCode("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/vs/prescription-reason", "38341003", "High blood pressure");
        assertTrue(validationResponse.isSuccessful());

    }


    @Test
    public void shouldValidatePrescriberMedicationInMedicationPrescription() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_substitution_type_reason.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertTrue("Medication-prescription,Prescriber pass through validation", validationResponse.isSuccessful());

    }

    @Test
    public void shouldValidateDispenseAndAdditionalInstructionsInMedicationPrescription() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_dispense_addinformation_valid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(1)).validateCode("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/additional-instructions",
                "79647ed4-a60e-4cf5-ba68-cf4d55956xyz", "Take With Water");
        assertTrue("Should Validate Valid Encounter In MedicationPrescription", validationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateInvalidDispenseInMedicationPrescription() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_dispense_addinformation_invalid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertFalse("Invalid Dispense Should Fail", validationResponse.isSuccessful());
        List<Error> errorList = CollectionUtils.filter(validationResponse.getErrors(), new CollectionUtils.Fn<Error, Boolean>() {
            @Override
            public Boolean call(Error e) {
                return e.getReason().equals(INVALID_DISPENSE_MEDICATION_REFERENCE_URL);
            }
        });

        assertEquals("Should Have Found One Invalid Dispense-Mediaction Url", 1, errorList.size());
    }

    @Test
    public void shouldValidateSubstitutionTypeAndReasonInMedicationPrescription() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_substitution_type_reason.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(1)).validateCode("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/vs/substitution-type", "291220002", "Paracetamol");
        verify(trConceptLocator, times(1)).validateCode("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/vs/substitution-reason", "301220005"
                , "Paracetamol can be taken in place of this drug");
        assertTrue(validationResponse.isSuccessful());


    }


    @Test
    public void shouldValidateMethodAndAsNeededXInMedicationPrescription() {

        /**
         * medication_prescription_route_valid.xml has
         * 2 medication prescribed with asNeeded (boolean true), and asNeeded with CodeableConcept
         *
         */
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_route_valid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(1)).validateCode("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr/concepts/79647ed4-a60e-4cf5-ba68-cf4d55956cba",
                "79647ed4-a60e-4cf5-ba68-cf4d55956cba", "Hemoglobin");
        verify(trConceptLocator, times(1)).validateCode("http://localhost:9997/openmrs/ws/rest/v1/tr/vs/administration-method-codes",
                "320276009", "Salmeterol+fluticasone 25/250ug inhaler");
        assertTrue(validationResponse.isSuccessful());

    }

    @Test
    public void shouldValidateInvalidDosageQuantityInMedicationPrescription() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/medication_prescription_invalid_dosage_quantity.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        List<Error> errors = validationResponse.getErrors();
        assertEquals("Invalid Dosage Quantity", 1, errors.size());

    }

    @Test
    @Ignore
    public void shouldValidateDischargeSummaryEncounterWithAllResources() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/discharge_summary_encounter.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        verify(trConceptLocator, times(31)).validateCode(contains("http://172.18.46.56:9080/openmrs/ws/rest/v1/tr"), anyString(), anyString());
        assertTrue(validationResponse.isSuccessful());
    }

    @Test
    public void shouldValidateInvalidDosageQuantityInDischargeSummaryEncounter() {
        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/discharge_summary_dosage_quantity_invalid.xml"));
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        List<Error> errors = validationResponse.getErrors();
        assertEquals("Invalid Dosage Quantity", 1, errors.size());
    }

    @Test
    public void shouldValidateInvalidCodeInDischargeSummaryEncounter() {
        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        when(trConceptLocator.validateCode(anyString(), eq("a6e20fe1-4044-4ce7-8440-577f7f814765-invalid"),
                anyString())).thenReturn(new ITerminologyServices.ValidationResult(OperationOutcome.IssueSeverity.ERROR,
                "Invalid code"));

        encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID,
                FileUtil.asString("xmls/encounters/discharge_summary_encounter_code_invalid.xml"));
        EncounterValidationResponse validationResponse = validator.validate(encounterBundle);
        assertFalse(validationResponse.isSuccessful());
        assertEquals(5, validationResponse.getErrors().size());
    }


}