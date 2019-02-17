package io.snice.modem.actors.fsm;

import io.hektor.fsm.Data;

public class FirmwareData implements Data {

    private boolean isResetting;

    public boolean isResetting() {
        return isResetting;
    }

    public void isResetting(final boolean value) {
        this.isResetting = value;
    }
}
