package io.snice.modem.actors.events;

import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.messages.management.impl.TransactionMessageImpl;

public class ModemConnectSuccess extends TransactionMessageImpl implements ModemMessage {

    private static final ModemConnectSuccess EVENT = new ModemConnectSuccess();

    public static final ModemConnectSuccess of() {
        return EVENT;
    }

    private ModemConnectSuccess() {
        // left empty intentionally
    }

    public boolean isConnectSuccessEvent() {
        return true;
    }
}
