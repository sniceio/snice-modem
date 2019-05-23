package io.snice.modem.actors.messages.management.impl;

import io.hektor.core.ActorRef;
import io.snice.buffer.Buffer;
import io.snice.modem.actors.messages.management.ManagementRequest;
import io.snice.modem.actors.messages.management.ManagementResponse.ConnectResponse;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class DefaultConnectRequest extends DefaultManagementRequest implements ManagementRequest.ConnectEvent {

    private final Buffer port;

    public static final ConnectEvent of(final Buffer port) {
        assertNotNull(port, "The port cannot be null or the empty string");
        return new DefaultConnectRequest(port);
    }

    private DefaultConnectRequest(final Buffer port) {
        this.port = port;
    }

    @Override
    public Buffer getPort() {
        return port;
    }

    @Override
    public ConnectResponse createErrorResponse(final String error) {
        return new ErrorConnectResponse(getTransactionId(), error);
    }

    @Override
    public ConnectResponse createSuccecssResponse(final ActorRef modem, final String portName) {
        assertNotNull(modem, "The referencec to the modem cannot be null");
        return new SuccessConnectResponse(getTransactionId(), modem, portName);
    }
}
