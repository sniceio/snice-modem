package io.snice.modem.actors.events;

import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.messages.management.impl.TransactionMessageImpl;

public class ModemDisconnect extends TransactionMessageImpl implements ModemMessage {

    private static final ModemDisconnect EVENT = new ModemDisconnect();

    public static ModemDisconnect of() {
        return EVENT;
    }

    private ModemDisconnect() {}

    @Override
    public boolean isDisconnectEvent() {
        return true;
    }

}
