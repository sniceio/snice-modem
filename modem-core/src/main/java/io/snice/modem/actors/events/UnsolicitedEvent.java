package io.snice.modem.actors.events;

import io.snice.buffer.Buffer;

/**
 * At any point in time, the modem may emit various kinds of events. These events
 * are typically information about the modems registration status, info when it changes
 * tracking area, incoming phone calls etc etc.
 */
public interface UnsolicitedEvent extends ModemEvent {

    /**
     * When we were unable to interpret the data coming off of the modem.
     * This means that you need to write another parser and register it.
     */
    interface UnknownData extends UnsolicitedEvent {

        /**
         * Obtain the raw data.
         */
        Buffer getData();
    }
}
