package sh.modem;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hektor.actors.io.ConsoleConfig;
import io.hektor.config.HektorConfiguration;
import io.snice.modem.actors.CommandConfiguration;
import io.snice.modem.actors.ModemConfiguration;

public class ShellConfig {

    @JsonProperty("hektor")
    private HektorConfiguration hektorConfig;

    @JsonProperty("console")
    private ConsoleConfig consoleConfig;

    @JsonProperty("modem")
    private ModemConfiguration modemConfiguration;

    @JsonProperty("commands")
    private CommandConfiguration commandConfiguration;

    public ModemConfiguration getModemConfiguration() {
        return modemConfiguration;
    }

    public CommandConfiguration getCommandConfiguration() {
        return commandConfiguration;
    }

    public HektorConfiguration getHektorConfig() {
        return hektorConfig;
    }

    public ConsoleConfig getConsoleConfig() {
        return consoleConfig;
    }

}
