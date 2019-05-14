package io.snice.modem.actors.messages.management.impl;

import io.snice.modem.actors.messages.management.ManagementRequest;

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
