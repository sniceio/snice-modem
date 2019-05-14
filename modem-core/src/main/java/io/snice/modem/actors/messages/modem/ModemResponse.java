package io.snice.modem.actors.messages.modem;

import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.TransactionMessage;

public interface ModemResponse extends TransactionMessage {

    default boolean isSuccess() {
        return false;
    }

    default boolean isFailure() {
        return false;
    }

    default boolean isAtResponse() { return false; }

    default AtResponse toAtResponse() {
        throw new ClassCastException("Unable to cast " + getClass().getName() + " to " + AtResponse.class.getName());
    }

}
