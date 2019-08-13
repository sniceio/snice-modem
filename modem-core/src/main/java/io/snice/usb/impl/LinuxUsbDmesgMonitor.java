package io.snice.usb.impl;

import io.hektor.actors.LoggingSupport;
import io.hektor.core.Actor;
import io.hektor.core.Props;
import io.snice.processes.Tail;
import io.snice.usb.UsbConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Simple actor that just monitors dmesg for usb devices being added/removed
 * and then issues events to anyone who is interested.
 */
public class LinuxUsbDmesgMonitor implements Actor, LoggingSupport {

    private static final Logger logger = LoggerFactory.getLogger(LinuxUsbDmesgMonitor.class);

    private final ExecutorService threadPool;
    private final UsbConfiguration config;
    private final LinuxUsbScanner scanner;

    private Process dmesg;

    // TODO: should come from configuration.
    // TODO: need to document how this works so it makes sense.
    private static final String USB_DEVICE_CONNECTED = "New USB device found, idVendor";
    // private final static Pattern DMESG_SYSFS_PATTERN = Pattern.compile("\\[.*\\] usb (\\d-\\d[\\.\\d]*): .*");

    private static final String USB_DEVICE_DISCONNECT = "USB disconnect, device number";

    public static Props props(final ExecutorService threadPool, final UsbConfiguration config, final LinuxUsbScanner scanner) {
        assertNotNull(threadPool, "The thread pool used for blocking IO operations cannot be null");
        assertNotNull(config, "The Configuration cannot be null");
        assertNotNull(scanner, "The Linux USB Scanner cannot be null");

        return Props.forActor(LinuxUsbDmesgMonitor.class, () -> new LinuxUsbDmesgMonitor(threadPool, config, scanner));
    }

    private LinuxUsbDmesgMonitor(final ExecutorService threadPool, final UsbConfiguration config, final LinuxUsbScanner scanner) {
        this.threadPool = threadPool;
        this.config = config;
        this.scanner = scanner;
    }

    @Override
    public void onReceive(final Object msg) {
        final var str = msg.toString();
        if (str.contains(USB_DEVICE_CONNECTED)) {
            scanner.processDmesgNewDevice(str);
        } else if (str.contains(USB_DEVICE_DISCONNECT)) {

        }
    }


    @Override
    public void start() {
        logInfo("Starting");
        final var self = self();
        final var tail = Tail.tailProcess("dmesg --follow")
                .withThreadPool(threadPool)
                .onNewLine(self::tell)
                .onError(System.err::println)
                .withFilter(".*New USB device found, idVendor.*|.*USB disconnect, device number.*")
                .build();
        tail.start();
    }

    @Override
    public void stop() {
        logInfo("Stopping");
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object getUUID() {
        return "dmesg";
    }
}
