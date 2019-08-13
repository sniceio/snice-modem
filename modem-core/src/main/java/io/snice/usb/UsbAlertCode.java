package io.snice.usb;

import io.hektor.actors.Alert;
import io.snice.usb.impl.LinuxUsbDmesgMonitor;

public enum UsbAlertCode implements Alert {

    /**
     * <p>
     * <b>Code:</b>10000
     * </p>
     *
     * <p>
     *     The Linux USB support relies on parsing messages from <code>dmesg</code> and if unable
     *     to do so the entire library will not function. There is a process that tails <code>dmesg</code> and
     *     as soon as a new device is detected that message is processed by the {@link LinuxUsbDmesgMonitor}.
     *     That string will contain the vendor and product id and if we are unable to parse that out, we are
     *     unable to handle that device. So, if this occurs you need to ensure that you configured the
     *     regexp correctly to handle the dmesg output.
     * </p>
     *
     *
     * <p>
     * Action:<br/>
     * Please check your configuration. You can run dmesg on the command line to figure out what you need.
     * </p>
     */
    UNABLE_TO_PARSE_DMESG_NEW_USB_DEVICE(10000,"Unable to parse the dmesg line \"{}\" using configured regexp of \"{}\". " +
            "Did the format change or did you not get the regular expression correct?");

    private final int code;
    private final String msg;

    UsbAlertCode(final int code, final String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }

    public int getCode() {
        return code;
    }
}
