package io.snice.modem.event;

import io.snice.protocol.RequestSupport;
import io.snice.protocol.ResponseSupport;
import io.snice.protocol.TransactionId;
import io.snice.usb.UsbDeviceDescriptor;

import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.ensureNotNull;

public interface ManagementEvent {

    static <T> ListRequest<T> listModems(final T owner) {
        return new ListRequest<>(ensureNotNull(owner));
    }

    class ListRequest<O> extends RequestSupport<O, Object> implements ManagementEvent {
        private ListRequest(final O owner) {
            super(owner);
        }

        public ModemList<O> createResponse(final List<UsbDeviceDescriptor> modems) {
            return new ModemList<O>(getTransactionId(), getOwner(), modems);
        }
    }

    class ModemList<O> extends ResponseSupport<O, List<UsbDeviceDescriptor>> {
        private ModemList(final TransactionId transactionId, final O owner, final List<UsbDeviceDescriptor> payload) {
            super(transactionId, owner, true, Optional.of(payload));
        }
    }

}
