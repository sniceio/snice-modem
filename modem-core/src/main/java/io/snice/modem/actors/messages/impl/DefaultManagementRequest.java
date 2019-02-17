package io.snice.modem.actors.messages.impl;

import io.snice.modem.actors.messages.ManagementRequest;

import java.util.UUID;

public abstract class DefaultManagementRequest implements ManagementRequest {
    private final UUID uuid;

    protected DefaultManagementRequest() {
        uuid = UUID.randomUUID();
    }

    @Override
    public UUID getTransactionId() {
        return uuid;
    }
}
