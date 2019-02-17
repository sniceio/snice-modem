package io.snice.modem.actors;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.actors.LoggingSupport;
import io.hektor.actors.io.InputStreamActor;
import io.hektor.actors.io.IoEvent;
import io.hektor.actors.io.OutputStreamActor;
import io.hektor.actors.io.StreamToken;
import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.hektor.core.internal.Terminated;
import io.snice.buffer.Buffer;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.events.ModemDisconnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
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
 *     of the modem, such as online/offline, start/shutdown etc. The {@link ModemActor} will delegate
 *     all executions of the actual {@link AtCommand}s to this {@link ModemFirmwareActor} and we will
 *     take care of any differences when it comes to reading/writing to/from the underlying modem.
 * </p>
 *
 */
public class ModemFirmwareActor implements Actor, LoggingSupport {
    private static final Logger logger = LoggerFactory.getLogger(ModemFirmwareActor.class);

    private final ActorRef self;

    private final ModemConfiguration config;
    private final ExecutorService blockingIoPool;
    private final SerialPort port;

    private final byte[] OK = new byte[]{Buffer.CR, Buffer.LF, (byte)'O', (byte)'K', Buffer.CR, Buffer.LF};

    private ActorRef inRef;
    private ActorRef outRef;

    public static Props props(final ModemConfiguration config, final SerialPort port, final ExecutorService blockingIpPool) {

        return Props.forActor(ModemFirmwareActor.class, () -> new ModemFirmwareActor(config, port, blockingIpPool));
    }

    private ModemFirmwareActor(final ModemConfiguration config, final SerialPort port, final ExecutorService blockingIpPool) {
        this.config = config;
        this.port = port;
        this.blockingIoPool = blockingIpPool;
        self = self();
    }

    @Override
    public void start() {
        logInfo("Starting");
        startReader();
        startWriter();
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

        if (msg instanceof AtCommand) {
            // pass to FSM
            outRef.tell(IoEvent.writeEvent(((AtCommand)msg).getCommand()), self());
        } else if (msg instanceof ModemDisconnect) {
            // should give this to the FSM as well.
            System.err.println("Guess we are shutting down");

        } else if (msg instanceof StreamToken) {
            // Should go to FSM
            processStreamToken((StreamToken)msg);
        } else if (msg instanceof Terminated) {
            // only one that shouldn't go to the FSM
            processChildDeath((Terminated)msg);
        }
    }

    private void processStreamToken(final StreamToken token) {
        final Buffer buffer = token.getBuffer();
        System.err.println("Received from modem: " + token.getBuffer());
        if (buffer.endsWith(OK)) {
            System.err.println("yay! it's an OK and the end of this CMD");
        }



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

}
