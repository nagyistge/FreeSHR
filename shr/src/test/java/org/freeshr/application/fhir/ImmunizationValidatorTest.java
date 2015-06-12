package org.freeshr.application.fhir;

import org.freeshr.config.SHRConfig;
import org.freeshr.config.SHREnvironmentMock;
import org.freeshr.util.ValidationMessageList;
import org.freeshr.validations.DoseQuantityValidator;
import org.freeshr.validations.ImmunizationValidator;
import org.freeshr.validations.UrlValidator;
import org.hl7.fhir.instance.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.model.ResourceType;
import org.hl7.fhir.instance.utils.ITerminologyServices;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.freeshr.utils.BundleHelper.getBundleEntry;
import static org.freeshr.validations.ValidationMessages.INVALID_DOSAGE_QUANTITY;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRConfig.class)
public class ImmunizationValidatorTest {

    @Mock
    private TRConceptLocator trConceptLocator;

    @Before
    public void setup() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldValidateImmunization() throws Exception {
        BundleEntryComponent bundleEntry = getBundleEntry("xmls/encounters/immunization/immunization_valid.xml",
                ResourceType.Immunization);

        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        ImmunizationValidator immunizationValidator = getValidator();

        List<ValidationMessage> validationMessages = immunizationValidator.validate(bundleEntry);

        assertTrue(new ValidationMessageList(validationMessages).isSuccessfull());
    }

    @Test
    public void shouldRejectInvalidDoseQuantityType() {
        BundleEntryComponent feed = getBundleEntry
                ("xmls/encounters/immunization/immunization_invalid_dose_quantity.xml", ResourceType.Immunization);

        when(trConceptLocator.verifiesSystem(anyString())).thenReturn(true);
        when(trConceptLocator.validateCode(anyString(), eq("INVALID-CODE"),
                anyString())).thenReturn(new ITerminologyServices.ValidationResult(OperationOutcome.IssueSeverity.ERROR,
                "Invalid code"));

        ImmunizationValidator immunizationValidator = getValidator();

        List<ValidationMessage> validationMessasges = immunizationValidator.validate(feed);

        ValidationMessageList messageList = new ValidationMessageList(validationMessasges);
        assertTrue(messageList.isOfSize(1));
        assertTrue(messageList.hasMessage(INVALID_DOSAGE_QUANTITY));
    }

    private ImmunizationValidator getValidator() {
        DoseQuantityValidator doseQuantityValidator = new DoseQuantityValidator(trConceptLocator);
        return new ImmunizationValidator(doseQuantityValidator, new UrlValidator());
    }
}
