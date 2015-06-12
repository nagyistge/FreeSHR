package org.freeshr.validations;

import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.config.SHRConfig;
import org.freeshr.config.SHREnvironmentMock;
import org.freeshr.util.ValidationMessageList;
import org.freeshr.utils.FileUtil;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRConfig.class)
public class HealthIdValidatorIT {

    @Autowired
    HealthIdValidator healthIdValidator;

    @Test
    public void shouldAcceptEncounterIfHealthIdInTheXmlMatchesTheGivenHealthId() {
        final String xml = FileUtil.asString("xmls/encounters/simple_valid_encounter.xml");
        List<ValidationMessage> validationMessages = healthIdValidator.validate(getEncounterContext(xml, "5893922485019082753"));
        assertTrue(new ValidationMessageList(validationMessages).isSuccessfull());
    }

    @Test
    public void shouldAcceptEncounterIfHealthIdInTheXmlMatchesTheGivenHealthIdAllVersions() {
        final String xml = FileUtil.asString("xmls/encounters/diagnostic_order_valid.xml");
        List<ValidationMessage> validationMessages = healthIdValidator.validate(getEncounterContext(xml, "5893922485019082753"));
        assertTrue(new ValidationMessageList(validationMessages).isSuccessfull());

    }

    @Test
    public void shouldRejectEncounterIfHealthIdInTheXmlDoesNotMatchTheGivenHealthId() {
        String xml = FileUtil.asString("xmls/encounters/simple_valid_encounter.xml");
        List<ValidationMessage> validationMessages = healthIdValidator.validate(getEncounterContext(xml, "11112222233333"));
        ValidationMessageList messageList = new ValidationMessageList(validationMessages);
        assertFalse(messageList.isSuccessfull());
        assertTrue(messageList.isOfSize(2));
        assertTrue(messageList.hasErrorOfTypeAndMessage("Patient's Health Id does not match.", Validator.ERROR_TYPE_INVALID));
    }

    private EncounterValidationContext getEncounterContext(final String xml, final String healthId) {
        EncounterBundle encounterBundle = new EncounterBundle();
        encounterBundle.setEncounterContent(xml);
        encounterBundle.setHealthId(healthId);
        return new EncounterValidationContext(encounterBundle);
    }
}