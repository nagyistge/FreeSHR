package org.freeshr.validations;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.freeshr.config.SHRConfig;
import org.freeshr.config.SHREnvironmentMock;
import org.freeshr.data.EncounterBundleData;
import org.freeshr.util.ValidationMessageList;
import org.hl7.fhir.instance.model.Bundle;
import org.hl7.fhir.instance.validation.ValidationMessage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.freeshr.utils.BundleHelper.getBundle;
import static org.freeshr.utils.FileUtil.asString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRConfig.class)

public class FacilityValidatorIT {

    @Autowired
    FacilityValidator validator;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9997);


    @Test
    public void shouldValidateEncounterIfItHasAValidFacility() throws Exception {
        givenThat(get(urlEqualTo("/facilities/10000069.json"))
                .withHeader("client_id", matching("18550"))
                .withHeader("X-Auth-Token", matching("c6e6fd3a26313eb250e1019519af33e743808f5bb50428ae5423b8ee278e6fa5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asString("jsons/facility10000069.json"))));

        Bundle bundle = getBundle("xmls/encounters/simple_valid_encounter.xml", EncounterBundleData.HEALTH_ID);

        List<ValidationMessage> validationMessages = validator.validate(bundle);

        assertTrue(validationMessages.isEmpty());
    }

    @Test
    public void shouldFailIfNotAValidFacility() throws Exception {
        givenThat(get(urlEqualTo("/facilities/100000603.json"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody(asString("jsons/facility100000603.json"))));

        Bundle bundle = getBundle("xmls/encounters/encounterWithInvalidFacility.xml", EncounterBundleData.HEALTH_ID);

        List<ValidationMessage> validationMessages = validator.validate(bundle);

        assertEquals(1, validationMessages.size());
        assertTrue(new ValidationMessageList(validationMessages).hasMessage(FacilityValidator.INVALID_SERVICE_PROVIDER));
    }

    @Test
    public void shouldFailIfFacilityUrlIsInvalid() throws Exception {
        Bundle bundle = getBundle("xmls/encounters/encounterWithInvalidFacilityUrl.xml", EncounterBundleData.HEALTH_ID);

        List<ValidationMessage> validationMessages = validator.validate(bundle);
        assertEquals(1, validationMessages.size());
        assertTrue(new ValidationMessageList(validationMessages).hasMessage(FacilityValidator.INVALID_SERVICE_PROVIDER_URL));
    }


}