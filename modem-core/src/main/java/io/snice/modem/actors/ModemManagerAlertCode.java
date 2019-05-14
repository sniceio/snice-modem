package io.snice.modem.actors;

import io.hektor.actors.Alert;
import io.snice.modem.actors.messages.management.ManagementRequest;

public enum ModemManagerAlertCode implements Alert {

    /**
     * If the {@link ModemManagerActor} receives an event it does not
     * handle, it will issue this warning.
     */
    UNKNOWN_MESSAGE_TYPE(1000, "Unhandled event of type {}"),

    /**
     * If the {@link ModemManagerActor} receives a {@link ManagementRequest}
     * event it does not * handle, it will issue this warning.
     */
    UNKNOWN_MANAGEMENT_MESSAGE(1001, "Unhandled management request. Event {}");

    private final int code;
    private final String msg;

    ModemManagerAlertCode(final int code, final String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        return msg;
    }

    @Override
    public int getCode() {
        return code;
    }
}
