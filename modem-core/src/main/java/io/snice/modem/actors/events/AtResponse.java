package io.snice.modem.actors.events;

import io.snice.buffer.Buffer;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Represents a raw response as read from the modem. The content is not
 * interpreted in anyway, it is just matched against the {@link AtCommand}
 * and the header and trailer of the response from the modem, as defined in xxxx,
 * has been stripped.
 *
 */
public class AtResponse extends ModemEvent {

    public static AtResponse of(final AtCommand cmd, final Buffer response) {
        assertNotNull(cmd, "The AT Command cannot be null");
        assertNotNull(response, "The response buffer cannot be null");
        return new AtResponse(cmd, response);
    }

    private final AtCommand cmd;
    private final Buffer response;

    /**
     *
     * @param cmd the command to which this response is responding :-)
     */
    private AtResponse(final AtCommand cmd, final Buffer response) {
        // we are responding to this transaction so use the same UUID
        super(cmd.getTransactionId());
        this.cmd = cmd;
        this.response = response;

    }

    @Override
    public boolean isAtResponse() {
        return true;
    }

    public AtResponse toAtResponse() {
        return this;
    }

    public Buffer getResponse() {
        return response;
    }
}
