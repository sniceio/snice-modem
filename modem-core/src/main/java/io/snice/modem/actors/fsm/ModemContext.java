package io.snice.modem.actors.fsm;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.core.ActorRef;
import io.hektor.fsm.Context;
import io.hektor.fsm.Scheduler;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.ModemEvent;
import io.snice.modem.actors.events.ModemReset;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ModemContext implements Context {

    private final Scheduler scheduler;
    private final ModemConfiguration config;
    private final ActorRef self;

    /**
     * The thread pool where we'll schedule various jobs, typically blocking IO-related ones...
     */
    private final ExecutorService threadPool;

    private final SerialPort port;

    private final List<ModemEvent> outstandingModemEvents;

    public ModemContext(final ActorRef self, final SerialPort serialPort, final Scheduler scheduler, final ExecutorService threadPool, final ModemConfiguration config) {
        this.self = self;
        this.port = serialPort;
        this.scheduler = scheduler;
        this.config = config;
        this.threadPool  = threadPool;
        this.outstandingModemEvents = new ArrayList<>();
    }

    public ActorRef getSelf() {
        return self;
    }

    /**
     * Connect to the modem.
     */
    public void connect() {

    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    public ModemConfiguration getConfig() {
        return config;
    }

    /**
     * Pass on an event to the actual modem. Typically, this will be AT commands but also
     * other types success commands such as the {@link ModemReset} command.
     *
     * @param event
     */
    public void sendEvent(final ModemEvent event) {
        outstandingModemEvents.add(event);
    }

    public Optional<ModemEvent> getNextModemEvent() {
        if (outstandingModemEvents.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(outstandingModemEvents.remove(0));
    }

    public SerialPort getPort() {
        return port;
    }

    public void runJob(final Callable<Object> job) {
        threadPool.submit(() -> {
            final Object result = job.call();
            return result;
        });
    }
}
