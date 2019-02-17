package io.snice.modem.actors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.hektor.actors.io.InputStreamConfig;
import io.hektor.actors.io.OutputStreamConfig;
import io.snice.buffer.Buffer;
import io.snice.modem.actors.events.AtCommand;

import java.util.Arrays;
import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotNull;

@JsonDeserialize(builder = ModemConfiguration.Builder.class)
public class ModemConfiguration {


    @JsonProperty("inputStream")
    private final InputStreamConfig inputStreamConfig;

    @JsonProperty("outputStream")
    private final OutputStreamConfig outputStreamConfig;

    @JsonProperty("baudRate")
    private final int baudRate;

    @JsonProperty("readTimeout")
    private final int readTimeout;

    /**
     * Whenever we need to reset the modem, which we'll do upon starting and if we run into
     * any errors and/or unknown state, we will execute these commands in the specified
     * order.
     */
    @JsonProperty("resetCommands")
    private List<AtCommand> resetCommands;

    public static Builder of() {
        return new Builder();
    }

    private ModemConfiguration(final int baudRate, final int readTimeout, final InputStreamConfig inputStreamConfig, final OutputStreamConfig outputStreamConfig) {
        this.baudRate = baudRate;
        this.readTimeout = readTimeout;
        this.inputStreamConfig = inputStreamConfig;
        this.outputStreamConfig = outputStreamConfig;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public InputStreamConfig getInputStreamConfig() {
        return inputStreamConfig;
    }

    public OutputStreamConfig getOutputStreamConfig() {
        return outputStreamConfig;
    }

    public static class Builder {

        /**
         * The default baud rate.
         */
        private final int DEFAULT_BAUD_RATE = 115200;

        private final int DEFAULT_READ_TIMEOUT = 0;

        private OutputStreamConfig outConfig;
        private InputStreamConfig inConfig;
        private final int baudRate = DEFAULT_BAUD_RATE;
        private final int readTimeout = DEFAULT_READ_TIMEOUT;

        private final Buffer append = Buffer.of(Buffer.CR, Buffer.LF);

        private List<AtCommand> resetCommands;

        private static final AtCommand[] DEFAULT_RESET_COMMANDS = new AtCommand[] {
                AtCommand.of("AT"),
                AtCommand.of("ATZ"),
                AtCommand.of("ATE"),
                AtCommand.of("ATV1")
        };

        public Builder() {
            // left empty so that jackson can create an
            // instance of this builder.
        }

        @JsonProperty("out")
        public Builder withOutputStreamConfig(final OutputStreamConfig config) {
            assertNotNull(config, "The output configuration cannot be null");
            outConfig = config;
            return this;
        }

        @JsonProperty("in")
        public Builder withInputStreamConfig(final InputStreamConfig config) {
            assertNotNull(config, "The input configuration cannot be null");
            inConfig = config;
            return this;
        }

        @JsonProperty("resetCommands")
        public Builder withResetCommands(final List<AtCommand> resetCommands) {
            assertNotNull(resetCommands, "The list of reset commands cannot be null");
            this.resetCommands = resetCommands;
            return this;
        }

        public ModemConfiguration build() {
            return new ModemConfiguration(baudRate, readTimeout, ensureInputConfig(), ensureOutputConfig());
        }

        private List<AtCommand> ensureResetCommands() {
            if (resetCommands == null || resetCommands.isEmpty()) {
                return Arrays.asList(DEFAULT_RESET_COMMANDS);
            }

            return resetCommands;
        }

        private InputStreamConfig ensureInputConfig() {
            if (inConfig != null) {
                return inConfig;
            }

            return InputStreamConfig.of().withParentAutoSubscribe(true).build();
        }

        private OutputStreamConfig ensureOutputConfig() {
            if (outConfig != null) {
                return outConfig;
            }

            return OutputStreamConfig.of().withParentAutoSubscribe(true).withAlwaysFlush(true).withAppend(append).build();
        }
    }

}
