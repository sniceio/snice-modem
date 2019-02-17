package io.snice.modem.actors.messages.impl;

import io.snice.modem.actors.messages.TransactionMessage;

import java.util.UUID;

public abstract class TransactionMessageImpl implements TransactionMessage {

    private final UUID uuid;

    public TransactionMessageImpl(final UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public final UUID getTransactionId() {
        return uuid;
    }
}
