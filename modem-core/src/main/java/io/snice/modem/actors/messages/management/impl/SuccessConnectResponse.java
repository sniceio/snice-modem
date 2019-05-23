package io.snice.modem.actors.messages.management.impl;

import io.hektor.core.ActorRef;
import io.snice.modem.actors.messages.management.ManagementResponse;

import java.util.Optional;
import java.util.UUID;

public class SuccessConnectResponse extends DefaultManagementResponse implements ManagementResponse.ConnectResponse {

    private final Optional<ActorRef> modem;
    private final String portName;

    protected SuccessConnectResponse(final UUID uuid, final ActorRef modem, final String portName) {
        super(uuid);
        this.modem = Optional.of(modem);
        this.portName = portName;
    }

    @Override
    public Optional<ActorRef> getModemRef() {
        return modem;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public String toString() {
        final var ref = modem.map(ActorRef::toString).orElse("N/A");
        return String.format("%s<%s, %s, %s>", SuccessConnectResponse.class.getSimpleName(), portName, ref, getTransactionId());
    }
}

