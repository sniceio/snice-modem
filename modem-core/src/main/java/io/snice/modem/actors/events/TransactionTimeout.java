package io.snice.modem.actors.events;

import io.snice.modem.actors.messages.TransactionMessage;
import io.snice.modem.actors.messages.modem.ModemRequest;
import io.snice.modem.actors.messages.modem.ModemResponse;

import java.util.UUID;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class TransactionTimeout implements ModemResponse {

    private final ModemRequest request;

    public static final TransactionTimeout of(final ModemRequest request) {
        assertNotNull(request, "The request cannot be null");
        return new TransactionTimeout(request);
    }

    public boolean matchTransaction(final TransactionMessage transaction) {
        return transaction != null && request.getTransactionId().equals(transaction.getTransactionId());
    }

    private TransactionTimeout(final ModemRequest request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return String.format("TransactionTimeout<%s>", request);
    }

    private static String format(final ModemRequest request) {
        if (request.isAtRequest()) {
            return request.toAtRequest().getCommand().toString();
        }

        return request.toString();
    }

    @Override
    public UUID getTransactionId() {
        return request.getTransactionId();
    }
}
