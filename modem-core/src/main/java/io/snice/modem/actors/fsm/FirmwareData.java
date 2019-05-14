package io.snice.modem.actors.fsm;

import io.hektor.actors.io.StreamToken;
import io.hektor.fsm.Data;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.modem.ModemResetRequest;
import io.snice.modem.actors.messages.modem.ModemResetResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.preconditions.PreConditions.assertNull;

public class FirmwareData implements Data {

    private boolean isResetting;

    private final List<AtCommand> outstandingCommands = new ArrayList<>();

    private List<AtCommand> currentResetCommands;

    private List<AtResponse> savedResetResponses;

    /**
     * We need to have this one saved separately because it will lead to many {@link AtCommand}s
     * being generated and eventually we'll respond back to this one with all the commands
     * that we executed.
     */
    private ModemResetRequest resetRequest;

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

    private StreamToken latestDataFromModem;

    /**
     * As we consume data from the modem, we will hopefully eventually
     * construct an AT response out success that data. Once we have done so
     * we'll save it here for further processing.
     */
    private AtResponse response;

    public AtResponse consumeResponse() {
        final AtResponse result = response;
        response = null;
        return result;
    }

    public boolean hasFinalResponse() {
        return response != null;
    }

    public void saveResponse(final AtResponse response) {
        assertNull(this.response, "We have unprocessed response, you have to process that response first");
        this.response = response;
    }

    /**
     * Because for the reset command we'll have potentially many commands/responses to save up before
     * we generate a {@link ModemResetResponse}
     */
    public void saveResetResponse(final AtResponse response) {
        if (savedResetResponses == null) {
            savedResetResponses = new ArrayList<>();
        }
        savedResetResponses.add(response);
    }

    public Optional<ModemResetRequest> consumeResetRequest() {
        final var maybe = Optional.ofNullable(resetRequest);
        resetRequest = null;
        return maybe;
    }

    public List<AtResponse> consumeResetResponse() {
        final var copy = List.copyOf(savedResetResponses);
        savedResetResponses = null;

        return copy;
    }

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

    public void saveStreamToken(final StreamToken token) {
        assertNull(latestDataFromModem, "We have unprocessed data from the modem. Can't save this object");
        latestDataFromModem = token;
    }

    public StreamToken consumeStreamToken() {
        final StreamToken token = latestDataFromModem;
        latestDataFromModem = null;
        return token;
    }

    public void saveResetRequest(final ModemResetRequest req) {
        assertNull(resetRequest, "You already have an outstanding reset request, that one must finish first");
        resetRequest = req;
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
     * have deteced the end success the stream, we'll consume them all and return it
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

    public Optional<AtCommand> getNextStashedCommand() {
        try {
            return Optional.of(outstandingCommands.remove(0));
        } catch (final IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    public boolean hasMoreResetCommands() {
        return currentResetCommands != null && !currentResetCommands.isEmpty();
    }

    public Optional<AtCommand> getNextResetCommand() {
        if (currentResetCommands == null || currentResetCommands.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(currentResetCommands.remove(0));
    }
}
