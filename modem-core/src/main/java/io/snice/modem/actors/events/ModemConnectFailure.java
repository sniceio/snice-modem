package io.snice.modem.actors.events;

public class ModemConnectFailure extends ModemEvent {

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
