package io.snice.modem.actors.messages.modem.impl;

import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.management.impl.TransactionMessageImpl;
import io.snice.modem.actors.messages.modem.ModemResetRequest;
import io.snice.modem.actors.messages.modem.ModemResetResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultModemResetRequest extends TransactionMessageImpl implements ModemResetRequest {

    @Override
    public ModemResetResponse createSuccessResponse(final List<AtResponse> resetResponses) {
        if (resetResponses == null || resetResponses.isEmpty()) {
            return new DefaultModemRestResponse(getTransactionId(), List.of());
        }

        return new DefaultModemRestResponse(getTransactionId(), resetResponses);
    }

    private static class DefaultModemRestResponse extends TransactionMessageImpl implements ModemResetResponse {

        private final List<AtResponse> resetResponses;

        private DefaultModemRestResponse(final UUID transaction, final List<AtResponse> resetResponses) {
            super(transaction);
            this.resetResponses = resetResponses;
        }

        @Override
        public List<AtResponse> getResetCommands() {
            return resetResponses;
        }

        @Override
        public String toString() {
            final var reset = resetResponses.isEmpty() ? "none" : resetResponses.stream().map(r -> r.getCommand().getCommand().toString()).collect(Collectors.joining(","));
            return String.format("%s<%s>", ModemResetResponse.class.getSimpleName(), reset);
        }

    }
}
