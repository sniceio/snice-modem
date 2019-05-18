package io.snice.modem.actors;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.actors.LoggingSupport;
import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.hektor.fsm.FSM;
import io.hektor.fsm.Scheduler;
import io.snice.modem.actors.events.FirmwareCreatedEvent;
import io.snice.modem.actors.fsm.CachingExecutorService;
import io.snice.modem.actors.fsm.CachingExecutorService.CallableHolder;
import io.snice.modem.actors.fsm.CachingFsmScheduler;
import io.snice.modem.actors.fsm.CachingFsmScheduler2;
import io.snice.modem.actors.fsm.ModemContext;
import io.snice.modem.actors.fsm.ModemData;
import io.snice.modem.actors.fsm.ModemFsm;
import io.snice.modem.actors.fsm.ModemState;
import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.messages.modem.ModemRequest;
import io.snice.modem.actors.messages.modem.ModemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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
    private CachingFsmScheduler2 cachingScheduler;

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

        this.blockingIoPool = blockingIoPool;

        // TODO: need to actually come from the yaml file!
        config = ModemConfiguration.of().build();
        modemCtx = new ActorModemContext(port);
        data = new ModemData();
    }

    @Override
    public void start() {
        logInfo("Starting");
        cachingScheduler = new CachingFsmScheduler2(ctx().scheduler(), self());
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
        logWarn(ModemAlertCode.UNHANDLED_FSM_EVENT, modemState, o.getClass().getName(), format(o));
    }

    public void onTransition(final ModemState currentState, final ModemState toState, final Object event) {
        logInfo("{} -> {} Event: {}", currentState, toState, format(event));
    }

    @Override
    public void onReceive(final Object msg) {
        fsm.onEvent(msg);
        postInvocation();
        if (fsm.isTerminated()) {
            logInfo("looks like the FSM is dead so shutting down...");
            ctx().stop();
        }
    }

    /**
     * After every invocation/interaction with the FSM we need to process
     * any runnables or timers the FSM may have scheduled.
     */
    private void postInvocation() {
        // processScheduledTasks();
        processCallables();
    }

    private void processCallables() {
        final List<CallableHolder> callables = cachingThreadPool.removeAll();
        final ActorRef self = self();
        if (callables != null) {
            for (final CallableHolder task : callables) {
                blockingIoPool.execute(() -> {
                    final CompletableFuture future = task.getFuture();
                    try {
                        final Object object = task.getCallable().call();
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
        return object.toString();
    }

    private class ActorModemContext implements ModemContext {

        private final SerialPort port;

        private final List<ModemMessage> outstandingModemEvents;

        private ActorModemContext(final SerialPort port) {
            this.port = port;
            this.outstandingModemEvents = new ArrayList<>();
        }

        @Override
        public void createFirmware(final ModemData.FirmwareType type) {
            final SerialPort port = modemCtx.getPort();
            firmwareActor = ctx().actorOf("firmware", ModemFirmwareActor.props(self(), config, port, blockingIoPool));
            self().tell(FirmwareCreatedEvent.of());
        }

        @Override
        public Scheduler getScheduler() {
            return cachingScheduler;
        }

        @Override
        public ModemConfiguration getConfig() {
            return config;
        }

        @Override
        public void send(final ModemRequest event) {
            if (firmwareActor != null) {
                firmwareActor.tell(event, sender());
            } else {
                logInfo("Unable to process {}, we are not connected to the modem", event);
            }
        }

        @Override
        public void onResponse(final ModemResponse response) {
            if (response.isAtResponse()) {
                System.err.println(response.toAtResponse().getResponse());
            } else {
                System.err.println(response);
            }
        }

        @Override
        public SerialPort getPort() {
            return port;
        }

        @Override
        public void runJob(final Callable<Object> job) {
            cachingThreadPool.submit(() -> {
                final Object result = job.call();
                return result;
            });
        }
    }
}
