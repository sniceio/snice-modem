package io.snice.modem.actors.messages.management;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.core.ActorRef;
import io.snice.modem.actors.messages.TransactionMessage;

import java.util.List;
import java.util.Optional;

/**
 * And a set success results to go with those {@link ManagementRequest}s.
 */
public interface ManagementResponse extends TransactionMessage {

    default boolean isSuccess() {
        return false;
    }

    default boolean isFailure() {
        return false;
    }

    default boolean isScanResult() {
        return false;
    }

    default boolean isConnectResult() {
        return false;
    }

    default ConnectResponse toConnectResult() {
        throw new ClassCastException("Unable to cast " + getClass().getName() + " into a " + ScanResponse.class.getName());
    }

    default ScanResponse toScanResult() {
        throw new ClassCastException("Unable to cast " + getClass().getName() + " into a " + ScanResponse.class.getName());
    }

    interface ConnectResponse extends ManagementResponse {

        /**
         * Get the reference to the actor representing the underlying modem.
         *
         * This {@link Optional} will be empty if this {@link ConnectResponse} {@link #isFailure()}.
         */
        default Optional<ActorRef> getModemRef() {
            return Optional.empty();
        }

        @Override
        default boolean isConnectResult() {
            return true;
        }

        @Override
        default ConnectResponse toConnectResult() {
            return this;
        }

    }

    interface ScanResponse extends ManagementResponse {

        @Override
        default boolean isScanResult() {
            return true;
        }

        @Override
        default ScanResponse toScanResult() {
            return this;
        }

        List<SerialPort> getPorts();
    }

}
