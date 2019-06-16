package io.snice.modem.actors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.snice.buffer.Buffers;
import io.snice.modem.actors.events.AtCommand;

import java.time.Duration;

import static io.snice.preconditions.PreConditions.assertArgument;

@JsonDeserialize(builder = CommandConfiguration.Builder.class)
public class CommandConfiguration {


    private final Duration defaultTimeout;

    public static Builder of() {
        return new Builder();
    }

    private CommandConfiguration(final Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public Duration getTimeout(final AtCommand cmd) {
        // hack for now
        if (cmd.getCommand().startsWithIgnoreCase(Buffers.wrap("at+cops=?"))) {
            return Duration.ofSeconds(180); // 3 min
        } else if (cmd.getCommand().startsWithIgnoreCase(Buffers.wrap("at+cops="))) {
            return Duration.ofSeconds(180); // 3 min
        } else if (cmd.getCommand().startsWithIgnoreCase(Buffers.wrap("atd"))) {
            return Duration.ofSeconds(30);
        }
        return defaultTimeout;
    }

    public static class Builder {

        /**
         * The default timeout for all commands. 5 seconds may seem long but it all
         * depends on the actual command. You really should define all timeouts
         * for all commands and probably need to do so for the type of modem as
         * well (?).
         */
        private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

        private Duration defaultTimeout;

        public Builder() {
            // left empty so that jackson can create an
            // instance success this builder.
        }

        @JsonProperty("defaultTimeoutMs")
        public Builder withDefaultTimeoutMs(final int timeout) {
            assertArgument(timeout > 0, "The default timeout cannot be negative, nor zero");
            defaultTimeout = Duration.ofMillis(timeout);
            return this;
        }

        public CommandConfiguration build() {
            return new CommandConfiguration(ensureDefaultTimeout());
        }

        private Duration ensureDefaultTimeout(){
            return defaultTimeout != null ? defaultTimeout : DEFAULT_TIMEOUT;
        }
    }

}
