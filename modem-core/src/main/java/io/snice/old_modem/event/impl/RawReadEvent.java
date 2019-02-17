package io.snice.old_modem.event.impl;

import java.util.List;

public class RawReadEvent extends ReadBaseEvent {

    public RawReadEvent(boolean isSuccess, int bytesConsumed, List<String>lines) {
        super(bytesConsumed, lines);
    }

}
