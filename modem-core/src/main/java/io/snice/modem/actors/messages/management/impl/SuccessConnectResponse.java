package io.snice.modem.actors.messages.management.impl;

import io.hektor.core.ActorRef;
import io.snice.modem.actors.messages.management.ManagementResponse;

import java.util.Optional;
import java.util.UUID;

public class SuccessConnectResponse extends DefaultManagementResponse implements ManagementResponse.ConnectResponse {

    private final Optional<ActorRef> modem;

    protected SuccessConnectResponse(final UUID uuid, final ActorRef modem) {
        super(uuid);
        this.modem = Optional.of(modem);
    }

    @Override
    public Optional<ActorRef> getModemRef() {
        return modem;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }
}

