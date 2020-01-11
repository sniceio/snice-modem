package io.snice.modem.hektor;

import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.snice.hektor.FsmActorContextSupport;
import io.snice.modem.fsm.ModemManagerContext;
import io.snice.usb.DeviceId;
import io.snice.usb.event.Subscribe;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Default implementation of the {@link ModemManagerContext} when executing
 * in an {@link Actor} environment/container provided by Hektor.io
 */
public class ActorModemManagerContext implements ModemManagerContext, FsmActorContextSupport {

    private final ActorRef self;
    private final ActorRef usbSubSystem;

    private ActorModemManagerContext(final ActorRef self, final ActorRef usbSubSystem) {
        this.self = self;
        this.usbSubSystem = usbSubSystem;
    }

    public static Builder withUsbSubSystem(final ActorRef usbSubSystem) {
        assertNotNull(usbSubSystem, "You must specify the USB sub-system");
        return new Builder(usbSubSystem);
    }

    @Override
    public void claim(final DeviceId device) {
        System.err.println("Claiming device");
    }

    @Override
    public void subscribe() {
        final var request = Subscribe.from(self).build();
        usbSubSystem.tell(request);
    }

    @Override
    public void reply(final Object msg) {
        sender().tell(msg, self);
    }

    public static class Builder {

        private final ActorRef usbSubSystem;
        private ActorRef self;

        private Builder(final ActorRef usbSubSystem) {
            this.usbSubSystem = usbSubSystem;
        }

        public Builder withSelf(final ActorRef self) {
            assertNotNull(self);
            this.self = self;
            return this;
        }

        public ActorModemManagerContext build() {
            assertNotNull(self, "You must specify the ActorRef to the Modem Manager");
            return new ActorModemManagerContext(self, usbSubSystem);
        }
    }
}
