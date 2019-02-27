package io.snice.modem.actors.events;

import java.util.UUID;

public abstract class ModemEvent {

    private final UUID transactionId;

    protected ModemEvent(final UUID transactionId) {
        this.transactionId = transactionId;
    }

    protected ModemEvent() {
        this(UUID.randomUUID());
    }

    public final UUID getTransactionId() {
        return transactionId;
    }

    public boolean isAtCommand() {
        return false;
    }

    public boolean isAtResponse() {
        return false;
    }

    public boolean isConnectEvent() {
        return false;
    }

    public boolean isConnectSuccessEvent() {
        return false;
    }

    public boolean isConnectFailureEvent() {
        return false;
    }

    public boolean isDisconnectEvent() {
        return false;
    }

    public AtResponse toAtResponse() {
        throw new ClassCastException("Unable to cast " + getClass().getName() + " to " + AtResponse.class.getName());
    }
}
