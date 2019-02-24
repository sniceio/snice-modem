package io.snice.modem.actors.fsm;

import io.hektor.fsm.Data;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.modem.actors.events.AtCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.preconditions.PreConditions.assertNull;

public class FirmwareData implements Data {

    private boolean isResetting;

    private final List<AtCommand> outstandingCommands = new ArrayList<>();

    private List<AtCommand> currentResetCommands;

    /**
     * The current command we have outstanding, i.e, we wrote it to the modem
     * and are waiting for return from the modem.
     */
    private AtCommand currentCommand;

    /**
     * All stashed data from the modem that we will consume once we detected
     * we've read all the data. Once we have, we'll consume it.
     */
    private final List<Buffer> dataFromModem = new ArrayList<>();

    public boolean isResetting() {
        return isResetting;
    }

    public void isResetting(final boolean value) {
        this.isResetting = value;
    }

    public AtCommand consumeCurrentCommand() {
        final AtCommand cmd = currentCommand;
        currentCommand = null;
        return cmd;
    }

    public void setCurrentCommand(final AtCommand cmd) {
        assertNotNull(cmd, "The AT command cannot be null");
        assertNull(currentCommand, "You cannot overwrite the current command we are wokring on. " +
                "You have to finish that command cycle first");
        currentCommand = cmd;
    }

    /**
     * When we start a new reset cycle we'll have to grab all the configured reset
     * commands.
     *
     * @param resetCommands
     */
    public void resetTheResetCommands(final List<AtCommand> resetCommands) {
        if (currentResetCommands == null) {
            currentResetCommands = new ArrayList<>();
        }

        currentResetCommands.addAll(resetCommands);
    }

    public void stashCommand(final AtCommand cmd) {
        assertNotNull(cmd, "The command cannot be null");
        outstandingCommands.add(cmd);
    }

    public boolean hasStashedCommands() {
        return !outstandingCommands.isEmpty();
    }

    /**
     * Save the current data we have received from the modem so far. Once we
     * have deteced the end of the stream, we'll consume them all and return it
     * to the caller.
     *
     * @param buffer
     */
    public void stashBuffer(final Buffer buffer) {
        dataFromModem.add(buffer);
    }

    public Buffer consumeAllData() {
        // TODO: should create a composite buffer
        // Or at the very least
        final Buffer buffer =  Buffers.wrap(dataFromModem);
        dataFromModem.clear();
        return buffer;
    }

    public Optional<AtCommand> getNextStashedCommands() {
        try {
            return Optional.of(outstandingCommands.remove(0));
        } catch (final IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    public Optional<AtCommand> getNextResetCommand() {
        if (currentResetCommands == null || currentResetCommands.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(currentResetCommands.remove(0));
    }
}
