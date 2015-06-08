package org.freeshr.infrastructure.tr;


import org.apache.commons.io.IOUtils;
import org.freeshr.utils.FileUtil;
import org.hl7.fhir.instance.formats.JsonParser;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.Medication;
import org.hl7.fhir.instance.model.StringType;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResourceExtensionTest {

    private final HashMap<String, String> extensions = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        extensions.put("http://192.168.33.17:9080/openmrs/ws/rest/v1/tr/medication#med-extension-strength", "500 mg");
        extensions.put("http://192.168.33.17:9080/openmrs/ws/rest/v1/tr/medication#med-extension-identifier", "123");
    }

    @Test
    public void shouldDeserializeMedicationWithExtension() throws Exception {
        String medicationJson = FileUtil.asString("jsons/medication_extn.json");
        Medication medication = (Medication)(new JsonParser().parse(IOUtils.toInputStream(medicationJson, "UTF-8")));

        List<Extension> extensions = medication.getExtension();
        assertEquals(2, medication.getExtension().size());
        for (Extension extension : extensions) {
            hasExtension(extension);
        }
    }

    private void hasExtension(Extension extension) {
        assertTrue(extensions.containsKey(extension.getUrl()));
        String value =  ((StringType) extension.getValue()).getValue();
        assertTrue(extensions.containsValue(value));
    }
}
