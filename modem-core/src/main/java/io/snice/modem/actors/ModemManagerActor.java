package io.snice.modem.actors;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.actors.LoggingSupport;
import io.hektor.core.Actor;
import io.hektor.core.Props;
import io.snice.buffer.Buffer;
import io.snice.modem.actors.messages.management.ManagementRequest;
import io.snice.modem.actors.messages.management.ManagementRequest.ConnectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Responsible for creating and managing new {@link ModemActor}s.
 */
public class ModemManagerActor implements Actor, LoggingSupport {

    private static final Logger logger = LoggerFactory.getLogger(ModemManagerActor.class);

    /**
     * Our thread pool for blocking IO tasks.
     */
    private final ExecutorService threadPool;

    private final Map<String, SerialPort> ports = new HashMap<>();

    public static Props props(final ExecutorService threadPool) {
        assertNotNull(threadPool, "The thread pool used for blocking IO operations cannot be null");
        return Props.forActor(ModemManagerActor.class, () -> new ModemManagerActor(threadPool));
    }

    private ModemManagerActor(final ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public void start() {
        logInfo("Starting");
    }

    @Override
    public void onReceive(final Object msg) {
        if (msg instanceof ManagementRequest) {
            processManagementEvent((ManagementRequest)msg);
        } else {
            logWarn(ModemManagerAlertCode.UNKNOWN_MESSAGE_TYPE, msg);
        }
    }

    private void processManagementEvent(final ManagementRequest event) {
        if (event.isScanEvent()) {
            final List<SerialPort> availablePorts = scan();
            sender().tell(event.toScanEvent().createResult(availablePorts), self());
        } else if (event.isConnectEvent()) {
            final ConnectEvent connect = event.toConnectEvent();
            final SerialPort port = getPort(connect.getPort());

            if (port == null) {
                sender().tell(connect.createErrorResponse("No such port"), self());
            } else {
                final var actorName = connect.getPort().toString();
                final var modem = ctx().actorOf(actorName, ModemActor.props(threadPool, port));
                sender().tell(connect.createSuccecssResponse(modem));
            }

        } else {
            logWarn(ModemManagerAlertCode.UNKNOWN_MANAGEMENT_MESSAGE, event);
        }
    }

    private SerialPort getPort(final Buffer port) {
        final SerialPort serialPort = ports.get(port.toString().toLowerCase());
        if (serialPort != null) {
            return serialPort;
        }

        scan();
        return ports.get(port.toString().toLowerCase());
    }

    // ugly, bloody side effect programming...
    private List<SerialPort> scan() {
        final SerialPort[] ports = SerialPort.getCommPorts();
        final List<SerialPort> listOfPorts;
        if (ports == null || ports.length == 0) {
            listOfPorts = Collections.EMPTY_LIST;
        } else {
            listOfPorts = Arrays.asList(ports);
            this.ports.clear();
            for (final SerialPort port : ports) {
                this.ports.put(port.getSystemPortName().toLowerCase(), port);
            }
        }

        return listOfPorts;
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
