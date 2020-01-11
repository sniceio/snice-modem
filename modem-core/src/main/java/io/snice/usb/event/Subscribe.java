package io.snice.usb.event;

import io.snice.usb.UsbDeviceDescriptor;

import java.util.function.Predicate;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Subscribe to events concerning USB devices added/removed from the system.
 */
public class Subscribe<T> {

    private final Predicate<UsbDeviceDescriptor> filter;
    private final T sender;

    private Subscribe(final T sender, final Predicate<UsbDeviceDescriptor> filter) {
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
            public Builder<T> withFilter(final Predicate<UsbDeviceDescriptor> filter) {
                assertNotNull(filter);
                return () -> new Subscribe<>(sender, filter);
            }

            @Override
            public Subscribe<T> build() {
                return new Subscribe<>(sender, id -> true);
            }
        };
    }

    public boolean accept(final UsbDeviceDescriptor descriptor) {
        return filter.test(descriptor);
    }

    public interface FilterStep<T> extends Builder<T> {
        Builder<T> withFilter(final Predicate<UsbDeviceDescriptor> filter);
    }

    public interface Builder<T> {
        Subscribe<T> build();
    }

}
