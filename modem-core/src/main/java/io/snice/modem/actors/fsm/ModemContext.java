package io.snice.modem.actors.fsm;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.fsm.Context;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.messages.modem.ModemRequest;
import io.snice.modem.actors.messages.modem.ModemResetRequest;
import io.snice.modem.actors.messages.modem.ModemResponse;

import java.util.UUID;
import java.util.concurrent.Callable;

public interface ModemContext extends Context {

    void createFirmware(final ModemData.FirmwareType type);

    ModemConfiguration getConfig();

    void onResponse(Transaction transaction, ModemResponse response);

    /**
     * Pass on an event to the actual modem. Typically, this will be AT commands but also
     * other types success commands such as the {@link ModemResetRequest} command.
     *
     * @param request
     */
    void send(final ModemRequest request);

    SerialPort getPort();

    void shutdownPort();

    void runJob(final Callable<Object> job);

    Transaction newTransaction(ModemRequest request);

    /**
     * For internal use only and for keeping track of the context
     * around the handling of a request/response.
     */
    interface Transaction {

        ModemRequest getRequest();

        default UUID getTransactionId() {
            return getRequest().getTransactionId();
        }

    }
}
