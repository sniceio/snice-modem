package io.snice.old_modem.modem;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import io.snice.buffer.Buffer;
import io.snice.buffer.ReadableBuffer;
import io.snice.buffer.impl.EmptyBuffer;
import io.snice.old_modem.event.ReadEvent;
import io.snice.old_modem.framers.Framer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleReader implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final SerialPort port;
    private final Thread thread;

    private final Config config;

    /**
     * This queue functions as our communication bus between processes.
     */
    private final BlockingQueue<ReadEvent> bus;

    private final CompletableFuture<SimpleReader> start = new CompletableFuture<>();
    private final CompletableFuture<SimpleReader> stop = new CompletableFuture<>();

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 10;

    private final Framer framer;

    /**
     * Hold enough data for, well, whatever...
     */

    public SimpleReader(final Config config, final Framer framer, final BlockingQueue<ReadEvent> bus, final SerialPort port) {
        this.config = config;
        this.framer = framer;
        this.bus = bus;
        this.port = port;
        this.thread = new Thread(this);
    }

    public CompletionStage<SimpleReader> start() {
        running.set(true);
        this.thread.start();
        return start;
    }

    public CompletionStage<SimpleReader> stop() {
        running.set(false);
        return stop;
    }

    @Override
    public void run() {
        start.complete(this);
        while (running.get()) {
            final ReadableBuffer buffer = read().toReadableBuffer();
            if (buffer.hasReadableBytes()) {
                System.err.println(buffer.dumpAsHex());
                this.framer.frame(buffer);
            }
        }
        System.err.println("Someone stopped me");
        stop.complete(this);
    }

    private void offerEvent(final ReadEvent event) {
        try {
            bus.offer(event, 100, TimeUnit.MILLISECONDS);
        } catch(final InterruptedException e) {
            System.err.println("Unable to insert read event - loosing data!!!");
        }
    }

    private Buffer read() {
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        try {
            final InputStream is = port.getInputStream();
            if (is == null) {
                return EmptyBuffer.EMPTY;
            }
            final int count = is.read(buffer, 0, buffer.length);
            if (count < 0) {
                return EmptyBuffer.EMPTY;
            }
            return Buffer.of(buffer, 0, count);
        } catch (final SerialPortTimeoutException e) {
            // System.err.println("Timed out while reading");
        } catch (final IndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return EmptyBuffer.EMPTY;
    }
}
