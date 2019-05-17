package io.snice.modem.actors.events;

import io.snice.modem.actors.messages.modem.ModemRequest;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class TransactionTimeout {

    private final ModemRequest request;

    public static final TransactionTimeout of(final ModemRequest request) {
        assertNotNull(request, "The request cannot be null");
        return new TransactionTimeout(request);
    }

    private TransactionTimeout(final ModemRequest request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return String.format("TransactionTimeout<%s, %s, %s>", request.getTransactionId(), request.getClass().getSimpleName(), format(request));
    }

    private static String format(final ModemRequest request) {
        if (request.isAtRequest()) {
            return request.toAtRequest().getCommand().toString();
        }

        return request.toString();
    }
}
