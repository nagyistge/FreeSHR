package org.freeshr.utils;

import org.hl7.fhir.instance.formats.XmlParser;
import org.hl7.fhir.instance.model.Bundle;

import java.io.ByteArrayInputStream;

public class BundleDeserializer {

    public BundleDeserializer() {
    }

    public Bundle deserialize(String xml) {
        try {
            return (Bundle) new XmlParser(true).parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
