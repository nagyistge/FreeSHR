package org.freeshr.validations;

import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.ResourceType;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.freeshr.utils.BundleHelper.getBundleEntry;
import static org.junit.Assert.*;

public class ProcedureValidatorTest {

    private ProcedureValidator procedureValidator;

    @Before
    public void setUp() {
        procedureValidator = new ProcedureValidator();
    }

    @Test
    public void shouldValidateProcedure() {

        BundleEntryComponent procedure = getBundleEntry("xmls/encounters/procedure/encounter_Procedure.xml",
                ResourceType.Procedure);
        List<ValidationMessage> validationMessages = procedureValidator.validate(procedure);
        assertTrue(validationMessages.isEmpty());
    }

    @Test
    public void shouldValidateDateInProcedure() {

        BundleEntryComponent procedure = getBundleEntry
                ("xmls/encounters/procedure/encounter_invalid_period_Procedure.xml", ResourceType.Procedure);
        List<ValidationMessage> validationMessages = procedureValidator.validate(procedure);
        assertFalse(validationMessages.isEmpty());
        assertEquals(ValidationMessages.INVALID_PERIOD, validationMessages.get(0).getMessage());
    }

    @Test
    public void shouldValidateDiagnosticReportResourceReference() {

        BundleEntryComponent procedure = getBundleEntry
                ("xmls/encounters/procedure/encounter_invalid_report_reference_Procedure.xml", ResourceType.Procedure);
        List<ValidationMessage> validationMessages = procedureValidator.validate(procedure);
        assertFalse(validationMessages.isEmpty());
        assertEquals(ValidationMessages.INVALID_DIAGNOSTIC_REPORT_REFERENCE, validationMessages.get(0).getMessage());
    }
}