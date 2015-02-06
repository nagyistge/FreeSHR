package org.freeshr.validations;

import org.freeshr.domain.ErrorMessageBuilder;
import org.freeshr.utils.BundleHelper;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.model.ResourceType;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ProcedureValidatorTest {

    private ProcedureValidator procedureValidator;

    @Before
    public void setUp() {
        procedureValidator = new ProcedureValidator(new DateValidator());
    }

    @Test
    public void shouldValidateProcedure() {

        Bundle.BundleEntryComponent bundleEntry  = BundleHelper.getBundleEntry("xmls/encounters/procedure/encounter_Procedure.xml", ResourceType.Procedure);
        List<ValidationMessage> validationMessages = procedureValidator.validate(bundleEntry);
        assertTrue(validationMessages.isEmpty());

    }

    @Test
    public void shouldValidateDateInProcedure() {

        Bundle.BundleEntryComponent bundleEntry  = BundleHelper.getBundleEntry("xmls/encounters/procedure/encounter_invalid_period_Procedure.xml", ResourceType.Procedure);
        List<ValidationMessage> validationMessages = procedureValidator.validate(bundleEntry);
        assertFalse(validationMessages.isEmpty());
        assertEquals(ErrorMessageBuilder.INVALID_PERIOD, validationMessages.get(0).getMessage());


    }

    @Test
    public void shouldValidateDiagnosticReportResourceReference() {

        Bundle.BundleEntryComponent bundleEntry  = BundleHelper.getBundleEntry("xmls/encounters/procedure/encounter_invalid_report_reference_Procedure.xml", ResourceType.Procedure);
        List<ValidationMessage> validationMessages = procedureValidator.validate(bundleEntry);
        assertFalse(validationMessages.isEmpty());
        assertEquals(ErrorMessageBuilder.INVALID_DIAGNOSTIC_REPORT_REFERNECE, validationMessages.get(0).getMessage());

    }

}