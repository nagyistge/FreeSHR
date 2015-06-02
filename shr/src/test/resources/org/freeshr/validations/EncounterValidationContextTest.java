package org.freeshr.validations;

import org.freeshr.application.fhir.EncounterBundle;
import org.freeshr.data.EncounterBundleData;
import org.freeshr.utils.FileUtil;
import org.freeshr.utils.BundleDeserializer;
import org.hl7.fhir.instance.model.AtomFeed;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class EncounterValidationContextTest {
    @Mock
    BundleDeserializer bundleDeserializer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldDeserialiseOnlyOnce() throws Exception {
        String content = FileUtil.asString("xmls/encounters/encounter_with_obs_valid.xml");
        EncounterBundle encounterBundle = EncounterBundleData.encounter(EncounterBundleData.HEALTH_ID, content);

        when(bundleDeserializer.deserialize(content)).thenReturn(new AtomFeed());

        EncounterValidationContext validationContext = new EncounterValidationContext(encounterBundle,
                bundleDeserializer);
        validationContext.getBundle();
        validationContext.getBundle();
        validationContext.getBundle();

        verify(bundleDeserializer, atMost(1)).deserialize(content);

    }
}