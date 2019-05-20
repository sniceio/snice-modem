package io.snice.modem.actors.fsm;

import io.hektor.fsm.Data;
import io.snice.modem.actors.messages.modem.ModemRequest;
import io.snice.modem.actors.messages.modem.ModemResponse;

import static io.snice.preconditions.PreConditions.assertNull;

public class ModemData implements Data {


    /**
     * We only expect to have a single outstanding transaction.
     * If we receive more requests while waiting for the completion
     * of an transaction we will just stash it.
     */
    private ModemContext.Transaction outstandingTransaction;

    private FirmwareType desiredFirmware = FirmwareType.GENERIC;

    public void setDesiredFirmware(final FirmwareType type) {
        desiredFirmware = type;
    }

    public FirmwareType getDesiredFirmware() {
        return desiredFirmware;
    }

    public void saveTransaction(final ModemContext.Transaction transaction) {
        assertNull(outstandingTransaction, "We already have an outstanding transaction");
        outstandingTransaction = transaction;
    }

    public boolean hasOutstandingTransaction() {
        return outstandingTransaction != null;
    }

    /**
     * Check to see if the {@link ModemResponse} matches the oustanding transaction
     * we have.
     *
     * @param response
     * @return
     */
    public boolean matchTransaction(final ModemResponse response) {
        return hasOutstandingTransaction()
                && outstandingTransaction.getTransactionId().equals(response.getTransactionId());
    }

    /**
     * Whenever the FSM has a matching transaction we'll consume it.
     */
    public ModemContext.Transaction consumeTransaction() {
        var t = outstandingTransaction;
        outstandingTransaction = null;
        return t;
    }


    public enum FirmwareType {
        GENERIC, SIERRA;
    }

}
