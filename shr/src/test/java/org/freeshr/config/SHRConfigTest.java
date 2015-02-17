package org.freeshr.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = SHREnvironmentMock.class, classes = SHRConfig.class)
public class SHRConfigTest {

    @Autowired
    @Qualifier("fhirTrMap")
    Properties fhirTrMap;

    @Test
    public void shouldLoadFhirTrMapping() throws Exception {
        assertEquals(fhirTrMap.get("http://hl7.org/fhir/vs/doc-codes"), "http://192.168.33.10:9080/openmrs/ws/rest/v1/tr/vs/doc-codes");
    }
}