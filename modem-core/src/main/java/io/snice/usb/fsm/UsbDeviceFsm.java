package io.snice.usb.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;

public class UsbDeviceFsm {

    public static final Definition<UsbDeviceState, UsbDeviceContext, UsbDeviceData> definition;

    static {
        final var builder = FSM.of(UsbDeviceState.class)
                .ofContextType(UsbDeviceContext.class)
                .withDataType(UsbDeviceData.class);

        definition = null;
    }
}
