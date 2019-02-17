package sh.modem;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hektor.actors.io.ConsoleConfig;
import io.hektor.config.HektorConfiguration;
import io.snice.modem.actors.ModemConfiguration;

public class ShellConfig {

    @JsonProperty("hektor")
    private HektorConfiguration hektorConfig;

    @JsonProperty("console")
    private ConsoleConfig consoleConfig;

    @JsonProperty("modem")
    private ModemConfiguration modemConfiguration;

    public ModemConfiguration getModemConfiguration() {
        return modemConfiguration;
    }

    public HektorConfiguration getHektorConfig() {
        return hektorConfig;
    }

    public ConsoleConfig getConsoleConfig() {
        return consoleConfig;
    }

}
