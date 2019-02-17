package io.snice.old_modem.modem;

import com.fazecast.jSerialComm.SerialPort;
import io.snice.old_modem.Command;
import io.snice.old_modem.Modem;
import io.snice.old_modem.command.AtCommand;
import io.snice.old_modem.event.ReadEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public class GenericModem implements Modem, Runnable {

    private final SerialPort port;

    private final BlockingQueue<ReadEvent> readBus;
    private final SimpleReader reader;

    private final Writer writer;

    private final Thread thread;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final CompletableFuture<Modem> start = new CompletableFuture<>();

    public GenericModem(final SerialPort port,
                        final BlockingQueue<ReadEvent> readBus,
                        final SimpleReader reader,
                        final Writer writer) {
        this.port = port;
        this.readBus = readBus;
        this.reader = reader;
        this.writer = writer;
        this.thread = new Thread(this);
    }

    @Override
    public void run() {
        start.complete(this);
        init();
        while (isRunning.get()) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public CompletionStage<Modem> start() {
        final CompletionStage<SimpleReader> readerStart = reader.start();
        final CompletionStage<Writer> writerStart = writer.start();

        // TODO: deal with the exceptional cases.
        return readerStart.thenCombine(writerStart, (r, w) -> {
            isRunning.set(true);
            this.thread.start();
            return this;
        }).thenCompose(m -> start);
    }

    /**
     * Initialize this modem with whatever default should be. It may be that
     * specific sub-classes of particular modems need to do something different
     * so then sub-class and override.
     *
     * @return
     */
    private CompletionStage<Command> init() {
        return sendCommand(AtCommand.of("ATZ"));
        // return writer.write(verboseResultCode).thenApply(aVoid -> this);

    }

    @Override
    public CompletionStage<Command> sendCommand(final String cmd) {
        return writer.write(AtCommand.of(cmd));
    }

    @Override
    public CompletionStage<Command> sendCommand(final Command cmd) {
        return null;
    }

    @Override
    public CompletionStage<Modem> stop() {
        isRunning.set(false);
        final CompletableFuture<Writer> w = writer.stop().toCompletableFuture();
        final CompletableFuture<SimpleReader> r = reader.stop().toCompletableFuture();
        return CompletableFuture.allOf(w, r).thenApply(aVoid -> {
            if (port.closePort()) {
                return this;
            }
            throw new RuntimeException("Unable to close down port");
        });
    }

    private void write(final Command cmd) {
        writer.write(cmd);
    }
}
