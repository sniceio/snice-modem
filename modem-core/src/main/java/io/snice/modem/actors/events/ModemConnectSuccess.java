package io.snice.modem.actors.events;

public class ModemConnectSuccess extends ModemEvent {

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
