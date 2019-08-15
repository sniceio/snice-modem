package io.snice.usb;

import io.hektor.actors.LoggingSupport;
import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.hektor.fsm.Context;
import io.hektor.fsm.Data;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.modem.actors.FirmwareAlertCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.preconditions.PreConditions.ensureNotNull;

/**
 * Quite often you want to have your {@link FSM} running within an actor. This provides the
 * execution environment for your FSM, default handling of logging etc.
 *
 */
public final class FsmActor<S extends Enum<S>, C extends Context, D extends Data> implements Actor, LoggingSupport {

    private static final Logger logger = LoggerFactory.getLogger(FsmActor.class);

    private final Definition<S, C, D> definition;

    private FSM<S, C, D> fsm;

    private Function<ActorRef, C> contextSupplier;
    private Supplier<D> dataSupplier;

    private C context;
    private D data;

    private OnStartFunction<C, D> onStart;
    private OnStopFunction<C, D> onStop;


    public static <S extends Enum<S>, C extends Context, D extends Data> Builder<S, C, D> of(final Definition<S, C, D> definition) {
        assertNotNull(definition, "The FSM definition cannot be null");
        return new Builder(definition);
    }

    private FsmActor(final Definition<S, C, D> definition,
                     final Function<ActorRef, C> context,
                     final Supplier<D> data,
                     final OnStartFunction<C, D> onStart,
                     final OnStopFunction<C, D> onStop) {
        this.definition = definition;
        this.contextSupplier = context;
        this.dataSupplier = data;
        this.onStart = onStart;
        this.onStop = onStop;
    }

    @Override
    public void start() {
        logInfo("Starting");
        // TODO: if these throw exception, we need to deal with and kill the actor.
        context = contextSupplier.apply(self());
        data = dataSupplier.get();
        onStart.start(ctx(), context, data);

        fsm = definition.newInstance(getUUID(), context, data, this::unhandledEvent, this::onTransition);
        fsm.start();
    }

    @Override
    public void stop() {
        logInfo("Stopping");
        onStop.stop(ctx(), context, data);
    }

    @Override
    public void postStop() {
        logInfo("Stopped");
    }

    @Override
    public void onReceive(final Object msg) {
        fsm.onEvent(msg);
        if (fsm.isTerminated()) {
            ctx().stop();
        }
    }

    public void unhandledEvent(final S state, final Object o) {
        logWarn(FirmwareAlertCode.UNHANDLED_FSM_EVENT, state, o.getClass().getName(), String.format("\"%s\"",format(o)));
    }

    public void onTransition(final S currentState, final S toState, final Object event) {
        logInfo("{} -> {} Event: {}", currentState, toState, format(event));
    }

    /**
     * TODO: perhaps we should allow the user to pass in their own formatting logic.
     *
     * @param object
     * @return
     */
    private static final String format(final Object object) {
        return object.toString();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object getUUID() {
        return null;
    }

    public static class Builder<S extends Enum<S>, C extends Context, D extends Data> {

        private final Definition<S, C, D> definition;

        private Function<ActorRef, C> context;
        private Supplier<D> data;

        private OnStartFunction<C, D> onStart;
        private OnStopFunction<C, D> onStop;

        private Builder(final Definition<S, C, D> definition) {
            this.definition = definition;
        }

        public Builder withContext(final C context) {
            assertNotNull(context, "The context cannot be null");
            this.context = (ref) -> context;
            return this;
        }

        public Builder withContext(final Supplier<C> context) {
            assertNotNull(context, "The context supplier cannot be null");
            this.context = (ref) -> context.get();
            return this;
        }

        public Builder withContext(final Function<ActorRef, C> context) {
            assertNotNull(context, "The context function cannot be null");
            this.context = context;
            return this;
        }


        public Builder withData(final D data) {
            assertNotNull(context, "The data cannot be null");
            this.data = () -> data;
            return this;
        }

        public Builder withData(final Supplier<D> data) {
            assertNotNull(context, "The data supplier cannot be null");
            this.data = data;
            return this;
        }

        public Builder withStartFunction(final OnStartFunction<C, D> onStart) {
            assertNotNull(onStart, "The on start function cannot be null");
            this.onStart = onStart;
            return this;
        }

        public Builder withStopFunction(final OnStopFunction<C, D> onStop) {
            assertNotNull(onStop, "The on stop function cannot be null");
            this.onStop = onStop;
            return this;
        }

        public Props build() {
            ensureNotNull(context, "You must supply the Context");
            ensureNotNull(data, "You must supply the Data");
            return Props.forActor(FsmActor.class, () ->
                    new FsmActor(definition, context, data, ensureOnStart(), ensureOnStop()));
        }

        private OnStartFunction<C, D> ensureOnStart() {
            if (onStart != null) {
                return onStart;
            }
            return (actorCtx, ctx, data) -> {};
        }

        private OnStopFunction<C, D> ensureOnStop() {
            if (onStop != null) {
                return onStop;
            }
            return (actorCtx, ctx, data) -> {};
        }

    }
}
