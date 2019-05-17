package io.snice.modem.actors.events;

/**
 *
 */
public final class FirmwareCreatedEvent {

    public static FirmwareCreatedEvent of() {
        return new FirmwareCreatedEvent();
    }

    private FirmwareCreatedEvent() {
        // intentionally left empty
    }
}
