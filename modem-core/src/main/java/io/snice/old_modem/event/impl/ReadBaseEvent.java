package io.snice.old_modem.event.impl;

import io.snice.old_modem.event.ReadEvent;

import java.util.List;

public abstract class ReadBaseEvent implements ReadEvent {

    private final List<String> lines;
    private final int bytesConsumed;

    protected ReadBaseEvent(final int bytesConsumed, final List<String> lines) {
        this.bytesConsumed = bytesConsumed;
        this.lines = lines;
    }

    @Override
    public List<String> getLines() {
        return lines;
    }

    @Override
    public int getBytesConsumed() {
        return bytesConsumed;
    }

}
