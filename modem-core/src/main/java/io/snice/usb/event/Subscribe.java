package io.snice.usb.event;

import io.snice.usb.DeviceId;

import java.util.function.Predicate;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Subscribe to events concerning USB devices added/removed from the system.
 */
public class Subscribe<T> {

    private final Predicate<DeviceId> filter;
    private final T sender;

    private Subscribe(final T sender, final Predicate<DeviceId> filter) {
        this.sender = sender;
        this.filter = filter;
    }

    public T getSender() {
        return sender;
    }

    public static <T> FilterStep<T> from(final T sender) {
        assertNotNull(sender);
        return new FilterStep<T>() {
            @Override
            public Builder<T> withFilter(final Predicate<DeviceId> filter) {
                assertNotNull(filter);
                return () -> new Subscribe<>(sender, id -> true);
            }

            @Override
            public Subscribe<T> build() {
                return new Subscribe<>(sender, id -> true);
            }
        };
    }

    public interface FilterStep<T> extends Builder<T> {
        Builder<T> withFilter(final Predicate<DeviceId> filter);
    }

    public interface Builder<T> {
        Subscribe<T> build();
    }

}
