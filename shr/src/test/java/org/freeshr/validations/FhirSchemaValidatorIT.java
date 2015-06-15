package org.freeshr.validations;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.freeshr.config.SHRConfig;
import org.freeshr.config.SHREnvironmentMock;
import org.freeshr.util.ValidationMessageList;
import org.freeshr.utils.FileUtil;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.freeshr.application.fhir.ValidationErrorType.STRUCTURE;
import static org.freeshr.utils.FileUtil.asString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRConfig.class)
public class FhirSchemaValidatorIT {

    @Autowired
    FhirSchemaValidator validator;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);

    @Before
    public void setUp() {
        givenThat(get(urlEqualTo("/openmrs/ws/rest/v1/tr/vs/doc-typecodes"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asString("jsons/tr/doc-typecodes.json"))));

    }

    @Test
    public void shouldNotAcceptEncounterIfNoHealthIdIsPresentInComposition() {
        givenThat(get(urlEqualTo("/facilities/10000069.json"))
                .withHeader("client_id", matching("18550"))
                .withHeader("X-Auth-Token", matching("c6e6fd3a26313eb250e1019519af33e743808f5bb50428ae5423b8ee278e6fa5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asString("jsons/facility10000069.json"))));

        givenThat(get(urlEqualTo("api/default/patients/5893922485019082753"))
                .withHeader("client_id", matching("18550"))
                .withHeader("X-Auth-Token", matching("c6e6fd3a26313eb250e1019519af33e743808f5bb50428ae5423b8ee278e6fa5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asString("jsons/patient_5893922485019082753.json"))));


        String xml = FileUtil.asString("xmls/encounters/invalid_composition.xml");
        List<ValidationMessage> validationMessages = validator.validate(xml);
        ValidationMessageList messageList = new ValidationMessageList(validationMessages);
        assertFalse(messageList.isEmpty());
        assertTrue(messageList.isOfSize(1));
        assertTrue(messageList.hasErrorOfTypeAndMessage("Element subject @ /f:Bundle/f:entry[1]/f:resource/f:Composition: min required = 1, but only found 0",
                STRUCTURE));
    }

    @Test
    public void shouldTreatFHIRWarningAsError() {
        String xml = FileUtil.asString("xmls/encounters/diagnosis_system_invalid.xml");


        List<ValidationMessage> validationMessages = validator.validate(xml);
        ValidationMessageList messageList = new ValidationMessageList(validationMessages);
//        assertFailureFromResponseErrors("/f:entry/f:content/f:Condition/f:Condition/f:category",
//                "None of the codes are in the expected value set http://hl7.org/fhir/vs/condition-category (http://hl7" +
//                        ".org/fhir/vs/condition-category)",
//                response.getErrors());
//        assertFailureFromResponseErrors("/f:entry/f:content/f:Condition/f:Condition/f:category/f:coding",
//                "Unknown Code System http://hl7.org/fhir/condition-category-invalid",
//                response.getErrors());
//        assertEquals(2, response.getErrors().size());
    }
}