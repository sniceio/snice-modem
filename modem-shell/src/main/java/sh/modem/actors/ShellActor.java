package sh.modem.actors;

import io.hektor.actors.LoggingSupport;
import io.hektor.actors.io.ConsoleActor;
import io.hektor.actors.io.IoEvent;
import io.hektor.actors.io.StreamToken;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;
import io.hektor.core.Request;
import io.hektor.core.Response;
import io.hektor.core.TransactionalActor;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.buffer.ReadableBuffer;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.TransactionMessage;
import io.snice.modem.actors.messages.management.ManagementRequest;
import io.snice.modem.actors.messages.management.ManagementRequest.ConnectEvent;
import io.snice.modem.actors.messages.management.ManagementResponse;
import io.snice.modem.actors.messages.management.ManagementResponse.ScanResponse;
import io.snice.modem.event.AvailableModems;
import io.snice.modem.event.ListModems;
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
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class ShellActor implements TransactionalActor, LoggingSupport {

    private static final Logger logger = LoggerFactory.getLogger(ShellActor.class);

    private static final Buffer AT_CMD = Buffers.wrap("AT");
    private static final Buffer CLAIM_CMD = Buffers.wrap("claim");
    private static final Buffer LIST_CMD = Buffers.wrap("list");
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
        } else if (msg instanceof AvailableModems) {
            processAvailableModems((AvailableModems)msg);
        } else if (msg instanceof ManagementResponse) {
            processModemManagementResult((ManagementResponse)msg);
        } else if (msg instanceof AtResponse) {
            System.err.println("yep: \n" + ((AtResponse) msg).toAtResponse().getResponse().toString());
        }
    }

    @Override
    public void onResponse(final Response response) {
        System.out.println("yay, got a response for " + response.getTransactionId());

    }

    private void processAvailableModems(final AvailableModems modems) {
        final var text = modems.getAvailableModems().stream().map(Object::toString).collect(Collectors.joining("\n"));
        console.tell(IoEvent.writeEvent("Available Modems: \n" + text), self());
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
            System.err.println("Ok, got the connect result: " + result.toConnectResult());
            modem = result.toConnectResult().getModemRef();
        }
    }

    private void processCommandLine(final StreamToken token) {
        final Buffer line = token.getBuffer();
        if (LIST_CMD.equalsIgnoreCase(line)) {
            final Request request = modemManager.request(new ListModems(), self());
            System.err.println("Requesting list modems " + request.getTransactionId());
        } else if (line.startsWith(CLAIM_CMD)) {
            processClaimCommand(line.toReadableBuffer());
        } else if (line.startsWithIgnoreCase(AT_CMD)) {
            processAtCommand(line);
        } else if (EXIT_CMD.equalsIgnoreCase(line)) {
            System.exit(1); // :-)
        }
    }

    private void processAtCommand(final Buffer cmd) {
        modem.ifPresent(ref -> {
            ref.tell(AtCommand.of(cmd), self());
        });
    }

    private void processClaimCommand(final ReadableBuffer line) {
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
