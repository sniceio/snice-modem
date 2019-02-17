package io.snice.modem.actors.messages;

import java.util.UUID;

/**
 * Base interface for all the various types of events that has a
 * transaction id associated with them.
 */
public interface TransactionMessage {

    /**
     * Every event has a unique identifier, which is then also included in the result object.
     *
     * @return
     */
    UUID getTransactionId();
}
