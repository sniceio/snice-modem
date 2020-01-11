package io.snice.modem.event;

import io.snice.fsm.Transaction;
import io.snice.fsm.TransactionId;
import io.snice.usb.DeviceId;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class ClaimModem<T> implements Transaction {

    private final T owner;
    private final DeviceId id;

    private ClaimModem(final T owner, final DeviceId id) {
        this.owner = owner;
        this.id = id;
    }

    public static <T> DeviceIdStep<T> forOwner(final T owner) {
        assertNotNull(owner);
        return id -> {
            assertNotNull(id);
            return new ClaimModem<>(owner, id);
        };
    }

    @Override
    public TransactionId getTransactionId() {
        return null;
    }

    interface DeviceIdStep<T> {
        ClaimModem<T> modemDeviceId(DeviceId id);
    }

    public DeviceId getDeviceId() {
        return id;
    }

    public T getOwner() {
        return owner;
    }

}
