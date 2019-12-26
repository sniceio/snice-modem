package io.snice.usb.hektor;

import io.hektor.core.ActorRef;
import io.snice.usb.event.Subscribe;
import io.snice.usb.fsm.support.Subscription;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * When we are executing within an actor environment, this
 * is the implementing {@link Subscription}, which then keeps track of
 * the {@link ActorRef} of the requestor.
 */
public class ActorSubscription implements Subscription {

    private final Subscribe<ActorRef> originalRequest;

    private ActorSubscription(final Subscribe<ActorRef> subscribeRequest) {
        this.originalRequest = subscribeRequest;
    }

    public static ActorSubscription of(final Subscribe request) {
        assertNotNull(request);
        try {
            final ActorRef sender = (ActorRef)request.getSender();
            return new ActorSubscription(request);
        } catch (final ClassCastException e) {
            // ugly!
            throw new IllegalArgumentException("The given " + Subscribe.class.getName()
                    + " is not of generic type " + ActorRef.class.getName());
        }
    }

    @Override
    public Subscribe getSubscribeRequest() {
        return originalRequest;
    }
}
