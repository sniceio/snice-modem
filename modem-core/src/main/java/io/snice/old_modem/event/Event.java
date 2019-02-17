package io.snice.old_modem.event;

public interface Event {

    default boolean isRead() {
        return false;
    }

    default boolean isWrite() {
        return false;
    }
}
