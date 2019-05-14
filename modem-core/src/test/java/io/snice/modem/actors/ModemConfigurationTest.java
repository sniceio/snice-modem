package io.snice.modem.actors;

import io.snice.modem.actors.events.AtCommand;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ModemConfigurationTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullResetCommandsArray() {
        ModemConfiguration.of().withResetCommands((AtCommand)null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullResetCommandsList() {
        final List<AtCommand> cmds = new ArrayList<>();
        cmds.add(null);
        ModemConfiguration.of().withResetCommands(cmds);
    }

}