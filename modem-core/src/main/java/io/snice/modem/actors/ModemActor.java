package io.snice.modem.actors;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.actors.LoggingSupport;
import io.hektor.actors.io.StreamToken;
import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.LifecycleEvent;
import io.hektor.core.Props;
import io.hektor.core.internal.Terminated;
import io.hektor.fsm.FSM;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.events.ModemConnectSuccess;
import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.fsm.CachingExecutorService;
import io.snice.modem.actors.fsm.CachingExecutorService.CallableHolder;
import io.snice.modem.actors.fsm.CachingFsmScheduler;
import io.snice.modem.actors.fsm.ModemContext;
import io.snice.modem.actors.fsm.ModemData;
import io.snice.modem.actors.fsm.ModemFsm;
import io.snice.modem.actors.fsm.ModemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Responsible for the over arching state and operation success the modem which it represents.
 */
public class ModemActor implements Actor, LoggingSupport {

    private static final Logger logger = LoggerFactory.getLogger(ModemActor.class);

    private final ModemContext modemCtx;
    private final ModemData data;

    private FSM<ModemState, ModemContext, ModemData> fsm;

    private final CachingExecutorService cachingThreadPool;
    private final CachingFsmScheduler cachingScheduler;

    private final ExecutorService blockingIoPool;

    private ActorRef firmwareActor;
    private final ModemConfiguration config;

    public static Props props(final ExecutorService blockingIoPool, final SerialPort port) {
        assertNotNull(blockingIoPool, "The thread pool used for blocking IO operations cannot be null");
        assertNotNull(port, "The serial port cannot be null");
        return Props.forActor(ModemActor.class, () -> new ModemActor(blockingIoPool, port));
    }

    private ModemActor(final ExecutorService blockingIoPool, final SerialPort port) {
        cachingThreadPool = new CachingExecutorService();
        cachingScheduler = new CachingFsmScheduler();

        this.blockingIoPool = blockingIoPool;
        config = ModemConfiguration.of().build();
        modemCtx = new ModemContext(self(), port, cachingScheduler, cachingThreadPool, config);
        data = new ModemData();
    }

    @Override
    public void start() {
        logInfo("Starting");
        fsm = ModemFsm.definition.newInstance(getUUID(), modemCtx, data, this::unhandledEvent, this::onTransition);
        fsm.start();
        postInvocation();
    }

    @Override
    public void stop() {

    }

    @Override
    public void postStop() {

    }


    public void unhandledEvent(final ModemState modemState, final Object o) {
        logWarn(ModemAlertCode.UNHANDLED_FSM_EVENT, modemState, o.getClass().getName(), format(0));
    }

    public void onTransition(final ModemState currentState, final ModemState toState, final Object event) {
        logInfo("{} -> {} Event: {}", currentState, toState, format(event));
    }

    @Override
    public void onReceive(final Object msg) {
        logInfo("Received a message: " + msg.getClass().getName());
        if (msg instanceof ModemConnectSuccess) {
            startFirmwareActor();
            fsm.onEvent(msg);
            postInvocation();
        } else if (msg instanceof AtCommand) {
            // Should we pass this via the Modem FSM as well? Not sure if it serves
            // any particular purpose at this point. The FirmwareFSM should keep track
            // of the things.
            //
            // E.g., if we do not get a response from the modem after some time, the FirmwareFSM
            // should try and recover. If it is unable to do so, perhaps it should just be killed
            // and then the ModemActor will process the actor death and, if it chooses to do so,
            // start over and try to reconnect again...
            sendToModem((AtCommand) msg);
        } else if (msg instanceof StreamToken) {
            final StreamToken token = (StreamToken) msg;

            System.err.println("Received from modem: " + token.getBuffer());
        } else if (msg instanceof LifecycleEvent.Terminated) {
            final LifecycleEvent.Terminated dead = (LifecycleEvent.Terminated) msg;
            logInfo("Received a Lifecycle Terminated event: {}", dead.getActor());
        } else if (msg instanceof Terminated) {
            processChildDeath((Terminated) msg);
        } else if (msg instanceof AtResponse) {
            System.err.println(msg);
        } else {
            logInfo("Passing event to Modem FSM");

            fsm.onEvent(msg);
            postInvocation();
            if (fsm.isTerminated()) {
                logInfo("looks like the FSM is dead so shutting down...");
                ctx().stop();
            }
        }
    }

    private void processChildDeath(final Terminated terminated) {
        // TODO: we should start the firmware actor once again
        // unless we have already done so X number of times, at which
        // point we will give up and "die" ourselves and let the next
        // level handle it...

    }

    private void startFirmwareActor() {
        final SerialPort port = modemCtx.getPort();
        firmwareActor = ctx().actorOf("firmware", ModemFirmwareActor.props(self(), config, port, blockingIoPool));
    }

    /**
     * After every invocation/interaction with the FSM we need to process
     * any runnables or timers the FSM may have scheduled.
     */
    private void postInvocation() {
        processAtCommands();
        processScheduledTasks();
        processRunnables();
        processCallables();
    }

    private void processAtCommands() {
        modemCtx.getNextModemEvent().ifPresent(this::sendToModem);
    }

    private void sendToModem(final ModemMessage event) {
        if (firmwareActor != null) {
            firmwareActor.tell(event, sender());
        } else {
            logInfo("Unable to process {}, we are not connected to the modem", event);
        }
    }

    private void processScheduledTasks() {
        for (final CachingFsmScheduler.CancellableTask task : cachingScheduler.drainAllScheduledTasks()) {
            logDebug("Processing scheduled task {}", task);
        }
    }

    private void processRunnables() {
    }

    private void processCallables() {
        logDebug("Processing any potential callables after invocation");
        final List<CallableHolder> callables = cachingThreadPool.removeAll();
        final ActorRef self = self();
        if (callables != null) {
            for (final CallableHolder task : callables) {
                blockingIoPool.execute(() -> {
                    final CompletableFuture future = task.getFuture();
                    try {
                        System.err.println("Executing another callable");
                        final Object object = task.getCallable().call();
                        System.err.println("Done: " + object.getClass().getName());
                        future.complete(object);
                        self.tell(object, self);
                    } catch (final Throwable t) {
                        t.printStackTrace();
                        future.completeExceptionally(t);
                    }
                });
            }
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object getUUID() {
        return self();
    }

    private static final String format(final Object object) {
        try {
            final ModemMessage event = (ModemMessage) object;
            return event.toString();
        } catch (final ClassCastException e) {
            return object.toString();
        }
    }
}
