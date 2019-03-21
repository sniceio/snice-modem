package io.snice.modem.actors;

import io.snice.ConfigurationTestBase;
import org.junit.Test;

import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CommandConfigurationTest extends ConfigurationTestBase {

    @Test
    public void testBasicConfiguration() throws Exception {
        final CommandConfiguration config = loadConfiguration("command-configuration001.yml", CommandConfiguration.class);
        assertThat(config.getDefaultTimeout(), is(Duration.ofMillis(1000)));
    }

}