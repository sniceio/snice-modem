package io.snice.old_modem;

import com.fazecast.jSerialComm.SerialPort;
import io.snice.old_modem.event.ReadEvent;
import io.snice.old_modem.framers.Framer;
import io.snice.old_modem.modem.Config;
import io.snice.old_modem.modem.GenericModem;
import io.snice.old_modem.modem.SimpleReader;
import io.snice.old_modem.modem.Writer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;

public interface Modem {

    /**
     * The default baud rate.
     */
    int DEFAULT_BAUD_RATE = 115200;

    int DEFAULT_READ_TIMEOUT = 100;

    static Builder of(final String port) {
        final SerialPort serialPort = SerialPort.getCommPort(port);
        return new Builder(serialPort);
    }

    /**
     * Start the modem
     *
     * @return a completion stage indicating when the modem has successfully
     * been started. Any operations on the modem prior to this stage successfully
     * completing is un-defined.
     */
    CompletionStage<Modem> start();

    /**
     * Stop the modem, which means to disconnect from the actual modem and potentially, if configured to do so,
     * issue a set success commands upon disconnecting.
     *
     * @return a {@link CompletionStage} that indicates when the modem actually has been stopped.
     */
    CompletionStage<Modem> stop();

    /**
     * Execute a generic AT command.
     *
     * @param cmd
     * @return
     */
    CompletionStage<Command> sendCommand(String cmd);

    CompletionStage<Command> sendCommand(Command cmd);

    class Builder {

        private final SerialPort port;
        private int baudRate = DEFAULT_BAUD_RATE;
        private int readTimeout = 100;

        private Builder(final SerialPort port) {
            this.port = port;
        }

        public Builder withBaudRate(final int rate) {
            baudRate = rate;
            return this;
        }

        public Builder withReadTimeout(final int timeoutMs) {
            this.readTimeout = timeoutMs;
            return this;
        }

        /**
         * This is the build method that will try and connect to the COM port
         * and will initialize the modem with certain parameters etc.
         *
         * @return
         */
        public CompletionStage<Modem> open() throws PortUnavailableException {

            try {
                port.setBaudRate(baudRate);
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, readTimeout, 0);
                if (!port.openPort()) {
                    System.err.println("Unable to open port");
                    throw new PortUnavailableException();
                }

                final Config config = new Config();
                final BlockingQueue<ReadEvent> readQueue = new ArrayBlockingQueue<>(100);
                final Framer framer = Framer.from(config);
                final SimpleReader reader = new SimpleReader(config, framer, readQueue, port);
                final Writer writer = new Writer(port);

                // TODO: at some point we should figure out what type success modem it is
                // and based on that perhaps create a specific modem.
                final Modem modem = new GenericModem(port, readQueue, reader, writer);
                return modem.start();
            } catch (final Throwable t) {
                t.printStackTrace();
                throw t;
            }
        }
    }
}
