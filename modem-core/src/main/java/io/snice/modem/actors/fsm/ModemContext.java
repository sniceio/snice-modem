package io.snice.modem.actors.fsm;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.fsm.Context;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.messages.modem.ModemRequest;
import io.snice.modem.actors.messages.modem.ModemResetRequest;
import io.snice.modem.actors.messages.modem.ModemResponse;

import java.util.concurrent.Callable;

public interface ModemContext extends Context {

    void createFirmware(final ModemData.FirmwareType type);

    ModemConfiguration getConfig();

    void onResponse(ModemResponse response);

    /**
     * Pass on an event to the actual modem. Typically, this will be AT commands but also
     * other types success commands such as the {@link ModemResetRequest} command.
     *
     * @param request
     */
    void send(final ModemRequest request);

    SerialPort getPort();

    void runJob(final Callable<Object> job);
}
