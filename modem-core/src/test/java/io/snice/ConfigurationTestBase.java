package io.snice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.After;
import org.junit.Before;

import java.io.InputStream;

public class ConfigurationTestBase {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    public static <T> T loadConfiguration(final String resource, final Class<T> clazz) throws Exception {
        final InputStream yamlStream = ConfigurationTestBase.class.getResourceAsStream(resource);
        if (yamlStream == null) {
            throw new IllegalArgumentException("No such resource");
        }
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
        return mapper.readValue(yamlStream, clazz);
    }
}
