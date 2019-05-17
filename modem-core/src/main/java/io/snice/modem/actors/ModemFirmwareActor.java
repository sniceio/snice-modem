package io.snice.modem.actors;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.actors.LoggingSupport;
import io.hektor.actors.io.InputStreamActor;
import io.hektor.actors.io.OutputStreamActor;
import io.hektor.actors.io.StreamToken;
import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.hektor.core.internal.Terminated;
import io.hektor.fsm.FSM;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.fsm.CachingFsmScheduler;
import io.snice.modem.actors.fsm.FirmwareContext;
import io.snice.modem.actors.fsm.FirmwareData;
import io.snice.modem.actors.fsm.FirmwareFsm;
import io.snice.modem.actors.fsm.FirmwareState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * <p>
 *     Adapts reading & writing from the underlying modem in a transparent way. Ensures that
 *     {@link AtCommand}s are written to the modem and will map responses to an {@link AtResponse}
 *     and pass it back to the calling actor.
 * </p>
 *
 * <p>
 *     Compare this with the {@link ModemActor} who only deals with the high-level operations
 *     success the modem, such as online/offline, start/shutdown etc. The {@link ModemActor} will delegate
 *     all executions success the actual {@link AtCommand}s to this {@link ModemFirmwareActor} and we will
 *     take care success any differences when it comes to reading/writing to/from the underlying modem.
 * </p>
 *
 */
public class ModemFirmwareActor implements Actor, LoggingSupport {
    private static final Logger logger = LoggerFactory.getLogger(ModemFirmwareActor.class);

    /**
     * We will dispatch all {@link ModemMessage}s and unsolicited events to the parent.
     * It will be up to it to e.g. dispatch those messages to the original caller.
     * The firmware actor doesn't care. Note that in our running system, the parent will
     * most likely be the {@link ModemActor}.
     */
    private final ActorRef parent;
    private final ActorRef self;

    private final ModemConfiguration config;
    private final ExecutorService blockingIoPool;
    private final SerialPort port;

    private ActorRef inRef;
    private ActorRef outRef;

    private FSM<FirmwareState, FirmwareContext, FirmwareData> fsm;
    private FirmwareContext ctx;
    private FirmwareData data;
    private final CachingFsmScheduler cachingFsmScheduler;

    /**
     * TODO: should probably be moved into the FSM but that would most likely require some sort of
     * either AtCommand.reply so that we hide the Actors from the FSM or we simply make that into
     * a method on the Context, which can keep track of this and still hide it from the FSM.
     *
     * Note: default size of the map is probably just fine. Don't expect a lot of outstanding transactions
     * and even if we need to re-hash, this system is not really intended for a high performance type of
     * systems.
     */
    private final Map<UUID, ActorRef> outstandingTransactions = new HashMap<>();

    public static Props props(final ActorRef parent, final ModemConfiguration config, final SerialPort port, final ExecutorService blockingIpPool) {
        return Props.forActor(ModemFirmwareActor.class, () -> new ModemFirmwareActor(parent, config, port, blockingIpPool));
    }

    private ModemFirmwareActor(final ActorRef parent, final ModemConfiguration config, final SerialPort port, final ExecutorService blockingIpPool) {
        this.parent = parent;
        this.config = config;
        this.port = port;
        this.blockingIoPool = blockingIpPool;
        self = self();
        this.cachingFsmScheduler = new CachingFsmScheduler();
    }

    @Override
    public void start() {
        logInfo("Starting");
        startReader();
        startWriter();

        data = new FirmwareData();
        ctx = new FirmwareContext(cachingFsmScheduler, config, parent, self, outRef);
        fsm = FirmwareFsm.definition.newInstance(getUUID(), ctx, data, this::unhandledEvent, this::onTransition);
        fsm.start();
    }

    private void startReader() {
        final InputStream in = port.getInputStream();
        inRef = ctx().actorOf("in", InputStreamActor.props(in, blockingIoPool, config.getInputStreamConfig()));
    }

    private void startWriter() {
        final OutputStream out = port.getOutputStream();
        outRef = ctx().actorOf("out", OutputStreamActor.props(out, blockingIoPool, config.getOutputStreamConfig()));
    }

    @Override
    public void onReceive(final Object msg) {

        if (msg instanceof Terminated) {
            // only one that shouldn't go to the FSM. Or perhaps it should?
            processChildDeath((Terminated) msg);
        } else {
            fsm.onEvent(msg);
            processScheduledTasks(cachingFsmScheduler);
        }
    }

    private void processScheduledTasks(final CachingFsmScheduler scheduler) {
        scheduler.drainAllScheduledTasks().forEach(t -> {
            var event = t.getProducer().get();
            ctx().scheduler().schedule(event, self, self, t.getDelay());
        });

    }

    private void processChildDeath(final Terminated terminated) {
        System.err.println("One of my children died: " + terminated.actor());
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object getUUID() {
        return self;
    }

    public void unhandledEvent(final FirmwareState state, final Object o) {
        logWarn(FirmwareAlertCode.UNHANDLED_FSM_EVENT, state, o.getClass().getName(), String.format("\"%s\"",format(o)));
    }

    public void onTransition(final FirmwareState currentState, final FirmwareState toState, final Object event) {
        logInfo("{} -> {} Event: {}", currentState, toState, format(event));
    }

    private static final String format(final Object object) {
        if (object instanceof StreamToken) {
            final var buffer = ((StreamToken)object).getBuffer();
            return "StreamToken<bytes=" + buffer.capacity() + ">";
        }

        try {
            final ModemMessage event = (ModemMessage) object;
            return event.toString();
        } catch (final ClassCastException e) {
            return object.toString();
        }
    }

}
