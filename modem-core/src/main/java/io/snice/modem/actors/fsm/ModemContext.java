package io.snice.modem.actors.fsm;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.core.ActorRef;
import io.hektor.fsm.Context;
import io.hektor.fsm.Scheduler;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.AtCommand;

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

    private final List<AtCommand> outstandingAtCommands;

    public ModemContext(final ActorRef self, final SerialPort serialPort, final Scheduler scheduler, final ExecutorService threadPool, final ModemConfiguration config) {
        this.self = self;
        this.port = serialPort;
        this.scheduler = scheduler;
        this.config = config;
        this.threadPool  = threadPool;
        this.outstandingAtCommands = new ArrayList<>();
    }

    public ActorRef getSelf() {
        return self;
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    public ModemConfiguration getConfig() {
        return config;
    }

    public void sendAtCommand(final AtCommand command) {
        outstandingAtCommands.add(command);
    }

    public Optional<AtCommand> getNextCommand() {
        if (outstandingAtCommands.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(outstandingAtCommands.remove(0));
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
