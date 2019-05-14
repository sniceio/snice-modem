package io.snice.modem.actors;

import io.hektor.actors.Alert;
import io.snice.modem.actors.messages.modem.ModemMessage;

public enum FirmwareAlertCode implements Alert {

    /**
     * If the {@link io.snice.modem.actors.fsm.FirmwareFsm} receives an event for which there
     * is not defined transition, we will log the following warning. Any event that is "unhandled"
     * is a bug and should be addressed asap.
     *
     * The arguments to the formatting string below are:
     * <ol>
     *     <li>FSM State</li>
     *     <li>Class name of the event</li>
     *     <li>Event as a formatted string</li>
     * </ol>
     */
    UNHANDLED_FSM_EVENT(2000, "{} Unhandled event of type {}. Formatted output {}"),

    /**
     * External components will send us various {@link ModemMessage}s and at some point later
     * we should generate a response and send it back to the caller. However, if we do
     * generate a response but we cannot find the oustanding transaction, then we failed to
     * inform the caller and we now have a dropped event.
     *
     * This should be treated as a bug and should be fixed asap.
     *
     * The arguments to the formatting string below are:
     * <ol>
     *     <li>Transaction ID</li>
     *     <li>Class name of the event</li>
     *     <li>Event as a formatted string</li>
     * </ol>
     */
    UKNOWN_TRANSACTION(2001, "{} Unknown transaction for event {}. Formatted output {}");

    private final int code;
    private final String msg;

    FirmwareAlertCode(final int code, final String msg) {
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
