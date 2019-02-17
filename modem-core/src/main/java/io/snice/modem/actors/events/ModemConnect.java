package io.snice.modem.actors.events;

public class ModemConnect extends ModemEvent {

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
