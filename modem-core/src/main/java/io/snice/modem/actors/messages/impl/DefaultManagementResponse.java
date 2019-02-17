package io.snice.modem.actors.messages.impl;

import io.snice.modem.actors.messages.ManagementResponse;

import java.util.UUID;

public abstract  class DefaultManagementResponse extends TransactionMessageImpl implements ManagementResponse {

    protected DefaultManagementResponse(final UUID uuid) {
        super(uuid);
    }
}
