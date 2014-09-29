package org.freeshr.application.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Bundle;
import ca.uhn.fhir.parser.XmlParser;
import ca.uhn.fhir.validation.FhirValidator;
import org.freeshr.config.SHRConfig;
import org.freeshr.config.SHREnvironmentMock;
import org.freeshr.utils.FileUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRConfig.class)
public class FhirValidatorTest {
    @Test
    public void shouldValidateEncounterWhenInProperFormat() throws Exception {

        String encounterBundle = FileUtil.asString("xmls/encounters/encounter.xml");
        FhirContext theContext = new FhirContext();
        FhirValidator validator = theContext.newValidator();
        XmlParser xmlParser = new XmlParser(theContext);
        Bundle bundle = xmlParser.parseBundle(encounterBundle);
        validator.validate(bundle);
    }

    @Test
    public void shouldRejectEncounterWithInvalidConcept() {
        String encounterBundle = FileUtil.asString("xmls/encounters/diagnosis_system_invalid.xml");
        FhirContext theContext = new FhirContext();
        FhirValidator validator = theContext.newValidator();

        XmlParser xmlParser = new XmlParser(theContext);
        Bundle bundle = xmlParser.parseBundle(encounterBundle);
        validator.validate(bundle);
    }
//
//    @Test
//    public void shouldRejectEncounterWithInvalidConceptReferenceTerms() {
//        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
//        when(trConceptLocator.validate(anyString(), eq("INVALID_REFERENCE_TERM"), anyString())).thenReturn(new ConceptLocator.ValidationResult(OperationOutcome.IssueSeverity.error, "Invalid code"));
//
//        encounterBundle = EncounterBundleData.encounter("healthId", FileUtil.asString("xmls/encounters/invalid_ref.xml"));
//        assertFalse(validator.validate(encounterBundle.getEncounterContent().toString()).isSuccessful());
//    }
//
//    @Test
//    public void shouldRejectEncounterWithMissingSystemForDiagnosis() throws Exception {
//        encounterBundle = EncounterBundleData.encounter("healthId", FileUtil.asString("xmls/encounters/diagnosis_system_missing.xml"));
//        assertFalse(validator.validate(encounterBundle.getEncounterContent().toString()).isSuccessful());
//    }
//
//    @Test
//    public void shouldRejectEncountersWithDiagnosisHavingAllInvalidSystems() {
//        encounterBundle = EncounterBundleData.encounter("healthId", FileUtil.asString("xmls/encounters/diagnosis_system_invalid.xml"));
//        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle.getEncounterContent().toString());
//        assertFalse(encounterValidationResponse.isSuccessful());
//    }
//
//    @Test
//    public void shouldTreatFHIRWarningAsError() {
//        encounterBundle = EncounterBundleData.encounter("healthId", FileUtil.asString("xmls/encounters/diagnosis_system_invalid.xml"));
//        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle.getEncounterContent().toString());
//        assertFalse(encounterValidationResponse.isSuccessful());
//    }
//
//    @Test
//    public void shouldRejectInvalidDiagnosisCategory() {
//        encounterBundle = EncounterBundleData.encounter("healthId", FileUtil.asString("xmls/encounters/diagnosis_category_invalid.xml"));
//        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle.getEncounterContent().toString());
//        assertFalse(encounterValidationResponse.isSuccessful());
//    }
//
//    @Test
//    public void shouldValidateDiagnosticOrder() throws Exception {
//        encounterBundle = EncounterBundleData.encounter("healthId", FileUtil.asString("xmls/encounters/diagnostic_order_valid.xml"));
//        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
//        EncounterValidationResponse encounterValidationResponse = validator.validate(encounterBundle.getEncounterContent().toString());
//        verify(trConceptLocator, times(1)).verifiesSystem(anyString());
//        assertTrue(encounterValidationResponse.isSuccessful());
//    }
}
