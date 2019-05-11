package sh.modem.actors;

import io.hektor.actors.LoggingSupport;
import io.hektor.actors.io.ConsoleActor;
import io.hektor.actors.io.IoEvent;
import io.hektor.actors.io.StreamToken;
import io.hektor.core.Actor;
import io.hektor.core.ActorPath;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.buffer.ReadableBuffer;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.ManagementRequest;
import io.snice.modem.actors.messages.ManagementRequest.ConnectEvent;
import io.snice.modem.actors.messages.ManagementResponse;
import io.snice.modem.actors.messages.ManagementResponse.ConnectResponse;
import io.snice.modem.actors.messages.ManagementResponse.ScanResponse;
import io.snice.modem.actors.messages.TransactionMessage;
import io.snice.modem.actors.messages.impl.ErrorConnectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.modem.ShellConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class ShellActor implements Actor, LoggingSupport {

    private static final Logger logger = LoggerFactory.getLogger(ShellActor.class);

    private static final Buffer AT_CMD = Buffers.wrap("AT");
    private static final Buffer CONNECT_CMD = Buffers.wrap("connect");
    private static final Buffer SCAN_CMD = Buffers.wrap("scan");
    private static final Buffer EXIT_CMD = Buffers.wrap("exit");

    public static Props props(final ShellConfig config, final ExecutorService blockingIoPool, final ActorRef modemManager, final InputStream in, final OutputStream out) {
        assertNotNull(config, "You must specify the shell configuration");
        assertNotNull(blockingIoPool, "You must specify the thread pool used for blocking IO operations");
        assertNotNull(modemManager, "You must specify modem manager");
        assertNotNull(in, "You must specify the input stream from where we'll read commands");
        assertNotNull(out, "You must specify the out stream to which we'll print the result success commands");
        return Props.forActor(ShellActor.class, () -> new ShellActor(config, blockingIoPool, modemManager, in, out));
    }

    private final ShellConfig config;
    private final ExecutorService blockingIoPool;
    private final ActorRef modemManager;
    private Optional<ActorRef> modem = Optional.empty();
    private final InputStream in;
    private final OutputStream out;

    private ActorRef console;

    private final Map<UUID, TransactionMessage> oustandingTransactions;

    private ShellActor(final ShellConfig config, final ExecutorService blockingIoPool, final ActorRef modemManager, final InputStream in, final OutputStream out) {
        this.config = config;
        this.blockingIoPool = blockingIoPool;
        this.modemManager = modemManager;
        this.in = in;
        this.out = out;

        oustandingTransactions = new HashMap<>();
    }

    @Override
    public void start() {
        logInfo("Starting");
        console = ctx().actorOf("console", ConsoleActor.props(in, out, blockingIoPool, config.getConsoleConfig()));
    }

    @Override
    public void onReceive(final Object msg) {
        if (msg instanceof StreamToken) {
            processCommandLine((StreamToken)msg);
        } else if (msg instanceof ManagementResponse) {
            processModemManagementResult((ManagementResponse)msg);
        } else if (msg instanceof AtResponse) {
            System.err.println(((AtResponse) msg).toAtResponse().getResponse().toString());
        }
    }

    private void processModemManagementResult(final ManagementResponse result) {
        if (result.isScanResult()) {
            final ScanResponse scan = result.toScanResult();
            if (scan.getPorts().isEmpty()) {
                console.tell(IoEvent.writeEvent("No available ports found"), self());
            } else {
                scan.getPorts().forEach(port -> {
                    console.tell(IoEvent.writeEvent(port.getSystemPortName()), self());
                });
            }
        } else if (result.isConnectResult()) {
            // TODO: this is not pushed through just yet...
            System.err.println(result.toConnectResult());
            modem = result.toConnectResult().getModemRef();
        }
    }

    private void processCommandLine(final StreamToken token) {
        final Buffer line = token.getBuffer();
        if (SCAN_CMD.equalsIgnoreCase(line)) {
            modemManager.tell(ManagementRequest.scan(), self());
        } else if (line.startsWith(CONNECT_CMD)) {
            processConnectCommand(line.toReadableBuffer());
        } else if (line.startsWithIgnoreCase(AT_CMD)) {
            processAtCommand(line);
        } else if (EXIT_CMD.equalsIgnoreCase(line)) {
            System.exit(1); // :-)
        }
    }

    private void processAtCommand(final Buffer cmd) {

        // TODO: fix because we haven't pushed through the ModemConnect result back to the caller.
        if (modem.isEmpty()) {
            final var path = modemManager.path().createChild("ttyusb2");
            modem = ctx().lookup(path);
        }

        modem.ifPresent(ref -> {
            ref.tell(AtCommand.of(cmd), self());
        });
    }

    private void processConnectCommand(final ReadableBuffer line) {
        line.readUntilWhiteSpace(); // consume the "connect" word
        final Buffer port = line.toBuffer();
        final ConnectEvent connect = ManagementRequest.connect(port);
        modemManager.tell(connect, self());
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object getUUID() {
        return self();
    }
}
