package io.snice.old_modem.modem;

import com.fazecast.jSerialComm.SerialPort;
import io.snice.old_modem.Command;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class Writer implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final SerialPort port;
    private final Thread thread;

    private final BlockingQueue<WriteOp> bus = new ArrayBlockingQueue<>(100);

    private final CompletableFuture<Writer> start = new CompletableFuture<>();
    private final CompletableFuture<Writer> stop = new CompletableFuture<>();

    public Writer(final SerialPort port) {
        this.port = port;
        this.thread = new Thread(this);
    }

    public CompletionStage<Writer> start() {
        running.set(true);
        this.thread.start();
        return start;
    }

    public CompletionStage<Writer> stop() {
        running.set(false);
        return stop;
    }

    public CompletionStage<Command> write(final Command cmd) {
        assertNotNull(cmd, "You must specify a command");

        final WriteOp op = new WriteOp(cmd);
        try {
            bus.offer(op, 100, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            // TODO: throw something specific elluding to the fact that you are writing too fast.
            throw new RuntimeException("Ahhhh you're writing too fast!");
        }
        return op.future;
    }

    @Override
    public void run() {
        start.complete(this);
        while (running.get()) {
            try {
                consumeCommand().ifPresent(this::writeInternal);
            } catch (final Throwable t) {
                t.printStackTrace();
            }
        }
        stop.complete(this);
    }

    private void writeInternal(final WriteOp writeOp) {
        try {
            final Command cmd = writeOp.cmd;
            final OutputStream out = port.getOutputStream();
            System.err.println("Writing: " + cmd);
            out.write(cmd.convert());
            out.flush();
            writeOp.future.complete(cmd);
        } catch(final IOException e) {
            writeOp.future.completeExceptionally(e);
            e.printStackTrace();
        } catch(final Exception e) {
            writeOp.future.completeExceptionally(e);
            e.printStackTrace();
        }
    }

    private Optional<WriteOp> consumeCommand() {
        try {
            return Optional.ofNullable(bus.poll(10, TimeUnit.MILLISECONDS));
        } catch (final IllegalMonitorStateException e) {
            // seems to happen when you shut down. Haven't figured out why, seems odd.
            return Optional.empty();
        } catch (final InterruptedException e) {
            return Optional.empty();
        }
    }

    /**
     * Simple holder for the completable future and the actual command.
     */
    private static class WriteOp {

        public final CompletableFuture<Command> future;
        public final Command cmd;

        private WriteOp(final Command cmd) {
            this.future = new CompletableFuture<>();
            this.cmd = cmd;
        }
    }
}
