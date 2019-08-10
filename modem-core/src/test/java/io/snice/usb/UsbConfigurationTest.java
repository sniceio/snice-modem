package io.snice.usb;

import io.snice.ConfigurationTestBase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UsbConfigurationTest extends ConfigurationTestBase {

    @Test
    public void testWhiteList() throws Exception {
        final var config = loadConfiguration("usb_config001.yml", UsbConfiguration.class);

        // 0001 has just 0001 meaning we'll process everythiung from this
        // vendor.
        assertThat(config.processDevice("0001", "nisse"), is(true));

        // Sierra, 1199, has only one thing specified so it should be the only thing
        // we should process.
        assertThat(config.processDevice("1199", "3456"), is(true));
        assertThat(config.processDevice("1199", "nope"), is(false));

        // Random crap, should be false
        assertThat(config.processDevice("asdf", "nisse"), is(false));

    }

}
