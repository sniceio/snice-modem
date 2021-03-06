package io.snice.modem.actors.messages.management;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.core.ActorRef;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.modem.actors.messages.management.ManagementResponse.ConnectResponse;
import io.snice.modem.actors.messages.management.ManagementResponse.ScanResponse;
import io.snice.modem.actors.messages.TransactionMessage;
import io.snice.modem.actors.messages.management.impl.DefaultConnectRequest;
import io.snice.modem.actors.messages.management.impl.DefaultScanRequest;

import java.util.List;

/**
 * A set success events for managing modems, meaning things like "scan for more modems", "connect to port",
 * "disconnect modem" etc etc. Then there are individual modem commands, i.e. the "AT" commands.
 */
public interface ManagementRequest extends TransactionMessage {

    default boolean isScanEvent() {
        return false;
    }

    default ScanRequest toScanEvent() {
        throw new ClassCastException("Unable to cast " + getClass().getName() + " into a " + ScanRequest.class.getName());
    }

    default boolean isConnectEvent() {
        return false;
    }

    default ConnectEvent toConnectEvent() {
        throw new ClassCastException("Unable to cast " + getClass().getName() + " into a " + ConnectEvent.class.getName());
    }

    static ScanRequest scan() {
        return DefaultScanRequest.of();
    }

    static ConnectEvent connect(final Buffer port) throws IllegalArgumentException {
        return DefaultConnectRequest.of(port);
    }

    static ConnectEvent connect(final String port) throws IllegalArgumentException {
        return DefaultConnectRequest.of(Buffers.wrap(port));
    }

    interface ConnectEvent extends ManagementRequest {

        @Override
        default boolean isConnectEvent() {
            return true;
        }

        @Override
        default ConnectEvent toConnectEvent() {
            return this;
        }

        Buffer getPort();

        ConnectResponse createErrorResponse(String error);

        ConnectResponse createSuccecssResponse(ActorRef modem, String portName);
    }

    interface ScanRequest extends ManagementRequest {

        @Override
        default boolean isScanEvent() {
            return true;
        }

        @Override
        default ScanRequest toScanEvent() {
            return this;
        }

        ScanResponse createResult(List<SerialPort> ports);
    }
}
