package io.snice.modem.actors.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;

import java.util.Objects;

public class AtCommand extends ModemEvent {

    private final Buffer command;

    @JsonCreator
    public static AtCommand of(final String cmd) {
        return new AtCommand(Buffers.wrap(cmd));
    }

    public static AtCommand of(final Buffer cmd) {
        return new AtCommand(cmd);
    }

    @Override
    public boolean isAtCommand() {
        return true;
    }

    private AtCommand(final Buffer command) {
        this.command = command;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AtCommand atCommand = (AtCommand) o;
        return Objects.equals(command, atCommand.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command);
    }

    public Buffer getCommand() {
        return command;
    }


    @Override
    public String toString() {
        return command.toString();
    }
}
