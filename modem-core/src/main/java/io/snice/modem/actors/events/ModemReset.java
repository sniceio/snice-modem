package io.snice.modem.actors.events;

public class ModemReset extends ModemEvent {

    private static final ModemReset EVENT = new ModemReset();

    public static ModemReset of() {
        return EVENT;
    }

    private ModemReset() {}

    @Override
    public boolean isDisconnectEvent() {
        return true;
    }

}
