package io.snice.modem.actors.events;

public class ModemDisconnect extends ModemEvent {

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
