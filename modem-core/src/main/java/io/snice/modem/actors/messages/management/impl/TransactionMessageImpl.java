package io.snice.modem.actors.messages.management.impl;

import io.snice.modem.actors.messages.TransactionMessage;

import java.util.Objects;
import java.util.UUID;

public abstract class TransactionMessageImpl implements TransactionMessage {

    private final UUID uuid;

    public TransactionMessageImpl() {
        this(UUID.randomUUID());
    }

    public TransactionMessageImpl(final UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public final UUID getTransactionId() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionMessageImpl that = (TransactionMessageImpl) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
