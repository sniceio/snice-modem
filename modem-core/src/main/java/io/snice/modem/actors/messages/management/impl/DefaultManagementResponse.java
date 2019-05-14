package io.snice.modem.actors.messages.management.impl;

import io.snice.modem.actors.messages.management.ManagementResponse;

import java.util.UUID;

public abstract  class DefaultManagementResponse extends TransactionMessageImpl implements ManagementResponse {

    protected DefaultManagementResponse(final UUID uuid) {
        super(uuid);
    }
}
