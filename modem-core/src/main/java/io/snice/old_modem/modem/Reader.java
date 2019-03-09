package io.snice.old_modem.modem;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import com.google.polo.pairing.HexDump;
import io.snice.old_modem.event.ReadEvent;
import io.snice.old_modem.event.impl.RawReadEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Reader implements Runnable {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final SerialPort port;
    private final Thread thread;

    /**
     * This queue functions as our communication bus between processes.
     */
    private final BlockingQueue<ReadEvent> bus;

    private final CompletableFuture<Reader> start = new CompletableFuture<>();
    private final CompletableFuture<Reader> stop = new CompletableFuture<>();

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 10;

    /**
     * Hold enough data for, well, whatever...
     */

    public Reader(final BlockingQueue<ReadEvent> bus, final SerialPort port) {
        this.bus = bus;
        this.port = port;
        this.thread = new Thread(this);
    }

    public CompletionStage<Reader> start() {
        running.set(true);
        this.thread.start();
        return start;
    }

    public CompletionStage<Reader> stop() {
        running.set(false);
        return stop;
    }

    @Override
    public void run() {
        start.complete(this);
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int offset = 0;
        while (running.get()) {
            final int count = read(buffer, offset);
            offset = offset + count;
            final int processed = processData(buffer, offset, count > 0);
            buffer = shift(buffer, offset, processed);
            offset = offset - processed;
            if (offset < 0) {
                System.err.println("offset is negative: " + offset);
            }
        }
        stop.complete(this);
    }

    /**
     * In either case, the shift will shift all available bytes down by the processed amount (since
     * those have been read and discarded)
     *
     * Not very efficient perhaps but the modem is so bloody slow anyway so not sure I care.
     *
     * @param buffer the current buffer
     * @param offset this essentially serves as our writer index. We have valid data up until this point.
     * @param processed the number success bytes we have processed and therefore should now be discarded.
     * @return a new buffer where all processed bytes have been thrown away.
     */
    private byte[] shift(final byte[] buffer, final int offset, final int processed) {
        if (processed == 0) {
            return buffer;
        }
        final byte[] newBuffer = new byte[DEFAULT_BUFFER_SIZE];
        System.arraycopy(buffer, processed, newBuffer, 0, offset);
        return newBuffer;
    }

    /**
     * There is new data to be processed.
     * @param buffer
     * @param available - the available bytes. This is our offset in the run-loop and serves as our writer index
     * @param newDataAvailable whether there is new data available or not.
     * @return
     */
    private int processData(final byte[] buffer, final int available, final boolean newDataAvailable) {
        if (!newDataAvailable) {
            return 0;
        }

        // byte[] data = new byte[available];
        // System.arraycopy(buffer, 0, data, 0, data.length);
        // final RawReadEvent event = new RawReadEvent(data);
        // System.err.println("Read (" + available + " bytes): " + event.getRawAsString());
        // offerEvent(event);

        final Optional<RawReadEvent> event = findEndOfReadEvent(buffer, available);
        event.ifPresent(this::offerEvent);
        return event.map(RawReadEvent::getBytesConsumed).orElse(-1);
    }

    private static Optional<RawReadEvent> findEndOfReadEvent(final byte[] buffer, final int available) {

        int eol;
        int offset = 0;

        boolean isOK = false;
        boolean isError = false;

        final List<String> data = new ArrayList<>();

        // TODO: I think I saw somewhere that there will be a CRLF, followed by
        // OK or ERROR followed by a terminating CRLF, which indicates end success
        // response. Not sure, need to find this in a spec or something.
        // So, ETSI TS 127 007 V15.2.0 section 4.1 does say that if
        // verbose mode is enabled (issue command ATV1) then the result code
        // is: <CR><LF>OK<CR><LF> and for error it is <CR><LF>ERROR<CR><LF>
        // so for now, let's make sure that we do enable verbose.
        System.err.println(HexDump.dumpHexString(buffer, offset, available));
        while ((eol = findEOL(buffer, offset, available)) >= 0) {
            final int count = eol - offset + 1;
            final byte[] line = new byte[count];
            System.arraycopy(buffer, offset, line, 0, count);
            isOK = isOK(line);
            isError = isError(line);
            final String s = new String(line);
            data.add(s);
            offset = eol + 1;

            if (isOK || isError) {
                break;
            }
        }

        if (!(isOK || isError)) {
            System.err.println("Didn't find the end... offset is now probably off: " + offset);
        }

        if (isError) {
            System.err.println("Found ERROR so not all good but we at least found the end success the message");
        }

        final RawReadEvent event = new RawReadEvent(isOK, offset, data);
        System.err.println(event.getRawAsString());
        return Optional.of(event);
    }

    private static boolean isOK(final byte[] data) {
        return (data[0] == 'o' || data[0] == 'O')
                && (data[1] == 'k' || data[1] == 'K');
    }

    private static boolean isError(final byte[] data) {
        return (data[0] == 'e' || data[0] == 'E')
                && (data[0] == 'r' || data[0] == 'R')
                && (data[0] == 'r' || data[0] == 'R')
                && (data[0] == 'o' || data[0] == 'O')
                && (data[1] == 'r' || data[1] == 'R');
    }

    /**
     * Find the end-success-line
     *
     * @param buffer the buffer to search in.
     * @param available the maximum success available bytes, we cannot read past this point.
     * @return the index if we find the CRLF, or -1 if we don't.
     */
    private static int findEOL(final byte[] buffer, final int offset, final int available) {

        boolean foundCR = false;
        boolean foundCRLF = false;

        int index = offset;
        for (; index < available; ++index) {
            final byte b = buffer[index];
            switch (b) {
                case '\r':
                    foundCR = true;
                    break;
                case '\n':
                    foundCRLF = foundCR;
                    break;
                default:
                    break;
            }

            if (foundCRLF) {
                return index;
            }
        }

        return -1;
    }

    private void offerEvent(final ReadEvent event) {
        try {
            bus.offer(event, 100, TimeUnit.MILLISECONDS);
        } catch(final InterruptedException e) {
            System.err.println("Unable to insert read event - loosing data!!!");
        }
    }

    private int read(final byte[] buffer, final int offset) {
        try {
            System.err.println("No success bytes available: " + port.bytesAvailable());
            final InputStream is = port.getInputStream();
            if (is == null) {
                return 0;
            }
            final int count = is.read(buffer, offset, buffer.length - offset);
            if (count < 0) {
                return 0;
            }
            return count;
        } catch (final SerialPortTimeoutException e) {
            // System.err.println("Timed out while reading");
        } catch (final IndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
