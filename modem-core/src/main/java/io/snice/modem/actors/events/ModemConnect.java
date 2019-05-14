package io.snice.modem.actors.events;

import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.messages.management.impl.TransactionMessageImpl;

public class ModemConnect extends TransactionMessageImpl implements ModemMessage {

    private static final ModemConnect CONNECT = new ModemConnect();

    public static ModemConnect of() {
        return CONNECT;
    }

    private ModemConnect() {}

    @Override
    public boolean isConnectEvent() {
        return true;
    }
}
