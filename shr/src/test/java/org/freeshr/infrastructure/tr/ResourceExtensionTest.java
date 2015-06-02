package org.freeshr.infrastructure.tr;


import org.apache.commons.io.IOUtils;
import org.freeshr.utils.FileUtil;
import org.hl7.fhir.instance.formats.JsonParser;
import org.hl7.fhir.instance.model.Extension;
import org.hl7.fhir.instance.model.Medication;
import org.hl7.fhir.instance.model.StringType;
import org.junit.Test;

import java.util.List;

public class ResourceExtensionTest {
    @Test
    public void shouldDeserializeMedicationWithExtension() throws Exception {
        String medicationJson = FileUtil.asString("jsons/medication_extn.json");
        Medication medication = (Medication)(new JsonParser().parse(IOUtils.toInputStream(medicationJson, "UTF-8")));

        List<Extension> extensions = medication.getExtension();
        for (Extension extension : extensions) {
            System.out.println(extension.getUrl());
            String value =  ((StringType) extension.getValue()).getValue();
            System.out.println(value);
        }
    }
}
