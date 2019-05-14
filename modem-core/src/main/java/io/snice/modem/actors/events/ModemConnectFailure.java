package io.snice.modem.actors.events;

import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.messages.management.impl.TransactionMessageImpl;

public class ModemConnectFailure extends TransactionMessageImpl implements ModemMessage {

    private static final ModemConnectFailure EVENT = new ModemConnectFailure();

    public static final ModemConnectFailure of() {
        return EVENT;
    }

    private ModemConnectFailure() {
        // left empty intentionally
    }

    public boolean isConnectFailureEvent() {
        return true;
    }
}
