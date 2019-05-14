package io.snice.modem.actors.messages.modem;

import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.TransactionMessage;

import java.util.UUID;

public interface ModemMessage extends TransactionMessage {

    default boolean isAtCommand() {
        return false;
    }

    default boolean isAtResponse() {
        return false;
    }

    default boolean isConnectEvent() {
        return false;
    }

    default boolean isConnectSuccessEvent() {
        return false;
    }

    default boolean isConnectFailureEvent() {
        return false;
    }

    default boolean isDisconnectEvent() {
        return false;
    }

    default AtResponse toAtResponse() {
        throw new ClassCastException("Unable to cast " + getClass().getName() + " to " + AtResponse.class.getName());
    }
}

