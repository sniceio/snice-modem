package io.snice.modem.actors.messages.modem;

import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.management.impl.TransactionMessageImpl;
import io.snice.modem.actors.messages.modem.impl.DefaultModemResetRequest;

import java.util.List;

public interface ModemResetRequest extends ModemRequest {

    static ModemResetRequest of() {
        return new DefaultModemResetRequest();
    }

    ModemResetResponse createSuccessResponse(List<AtResponse> resetResponses);

    default ModemResetResponse createSuccessResponse() {
        return createSuccessResponse(List.of());
    }

}
