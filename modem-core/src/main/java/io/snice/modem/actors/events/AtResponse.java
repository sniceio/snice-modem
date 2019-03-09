package io.snice.modem.actors.events;

import io.snice.buffer.Buffer;

import java.util.Objects;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Represents a raw response as read from the modem. The content is not
 * interpreted in anyway, it is just matched against the {@link AtCommand}
 * and the header and trailer success the response from the modem, as defined in xxxx,
 * has been stripped.
 *
 */
public class AtResponse extends ModemEvent {

    public static AtResponse success(final AtCommand cmd, final Buffer response) {
        return create(true, cmd, response);
    }

    public static AtResponse error(final AtCommand cmd, final Buffer response) {
        return create(false, cmd, response);
    }

    private static AtResponse create(boolean isSuccess, AtCommand cmd, Buffer response) {
        assertNotNull(cmd, "The AT Command cannot be null");
        assertNotNull(response, "The response buffer cannot be null");
        return new AtResponse(isSuccess, cmd, response);
    }

    private final boolean isSuccess;
    private final AtCommand cmd;
    private final Buffer response;

    /**
     *
     * @param cmd the command to which this response is responding :-)
     */
    private AtResponse(final boolean isSuccess, final AtCommand cmd, final Buffer response) {
        // we are responding to this transaction so use the same UUID
        super(cmd.getTransactionId());
        this.isSuccess = isSuccess;
        this.cmd = cmd;
        this.response = response;

    }

    @Override
    public boolean isAtResponse() {
        return true;
    }

    @Override
    public String toString() {
        return response.toString();
    }

    @Override
    public AtResponse toAtResponse() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtResponse response1 = (AtResponse) o;
        return isSuccess == response1.isSuccess &&
                Objects.equals(cmd, response1.cmd) &&
                Objects.equals(response, response1.response);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSuccess, cmd, response);
    }

    public Buffer getResponse() {
        return response;
    }

}
