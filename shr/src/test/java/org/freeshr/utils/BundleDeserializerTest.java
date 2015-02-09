package org.freeshr.utils;

import org.hl7.fhir.instance.model.Bundle;
import org.junit.Test;

public class BundleDeserializerTest {

    @Test
    public void shouldDeserializeBundle() throws Exception {
        final String xml = FileUtil.asString("xmls/encounters/diagnostic_order_valid.xml");

        Bundle bundle = new BundleDeserializer().deserialize(xml);
        System.out.println(bundle);

    }
}