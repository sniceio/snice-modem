package sh.modem;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.hektor.config.HektorConfiguration;
import io.hektor.core.Hektor;
import io.hektor.core.Props;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.usb.ActorUsbManagerContext;
import io.snice.usb.FsmActor;
import io.snice.usb.OnStartFunction;
import io.snice.usb.UsbConfiguration;
import io.snice.usb.UsbScanner;
import io.snice.usb.fsm.UsbManagerContext;
import io.snice.usb.fsm.UsbManagerData;
import io.snice.usb.fsm.UsbManagerFsm;
import io.snice.usb.impl.DefaultUsbEvent;
import io.snice.usb.impl.JavaxUsbDeviceWrapper;
import io.snice.usb.impl.LinuxUsbDmesgMonitor;
import io.snice.usb.impl.LinuxUsbScanner;
import io.snice.usb.impl.UsbIdLoader;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Shell {

    public Shell() {

    }

    private static ShellConfig loadConfig(final String file) throws IOException {
        final Path configFile = Paths.get(file);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final SimpleModule module = new SimpleModule();

        module.addDeserializer(Buffer.class, new JsonDeserializer<Buffer>() {
            @Override
            public Buffer deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return Buffers.wrap(p.getValueAsString());
            }
        });
        mapper.registerModule(module);
        return mapper.readValue(configFile.toFile(), ShellConfig.class);
    }

    public static UsbDevice findDevice(final UsbHub hub, final short vendorId, final short productId)
    {
        for (final UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            final UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            System.err.println(desc);

            // if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;

            if (device.isUsbHub()) {
                System.err.println("yep, is hub");
                // device = findDevice((UsbHub) device, vendorId, productId);
                // if (device != null)
                    // return device;
            }
        }
        return null;
    }

    private static Props configureUsbManager(final ExecutorService blockingIoPool, final UsbConfiguration usbConfiguration) throws UsbException {

        final var knownUsbVendors = UsbIdLoader.load();
        final var scanner = LinuxUsbScanner.of(usbConfiguration, knownUsbVendors);

        final ActorUsbManagerContext usbManagerCtx = new ActorUsbManagerContext(scanner, usbConfiguration, knownUsbVendors);

        final UsbManagerData usbManagerData = new UsbManagerData();

        final OnStartFunction<UsbManagerContext, UsbManagerData> onStart = (actorCtx, ctx, data) -> {
            System.err.println("I guess the FSM is starting. This is pretty cool actually");
            var props = LinuxUsbDmesgMonitor.props(blockingIoPool, usbConfiguration, scanner);
            var usbMonitor = actorCtx.actorOf("monitor", props);
        };

        return FsmActor.of(UsbManagerFsm.definition)
                .withContext(usbManagerCtx)
                .withData(usbManagerData)
                .withStartFunction(onStart)
                .build();
    }

    public static void main(final String... args) throws Exception {
        // findDevice(null, (short)9, (short)9);


        // TODO: got to have a proper command line arg stuff.
        final ShellConfig shellConfig = loadConfig(args[0]);
        final HektorConfiguration hektorConfig = shellConfig.getHektorConfig();

        // TODO: should make it configurable.
        final ExecutorService blockingIoPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);

        final Hektor hektor = Hektor.withName("modem.sh").withConfiguration(hektorConfig).build();


        // final var props = FsmActor.of(UsbManagerFsm.definition).build();

        // final ActorRef usbManager = hektor.actorOf(UsbManagerActor.props(services, usbConfiguration), "usb");
        final var usbProps = configureUsbManager(blockingIoPool, shellConfig.getUsbConfiguration());
        hektor.actorOf(usbProps, "usb");

        // final ActorRef modemManager = hektor.actorOf(ModemManagerActor.props(blockingIoPool), "modem_manager");
        // final ActorRef shell = hektor.actorOf(ShellActor.props(shellConfig, blockingIoPool, modemManager, System.in, System.out), "shell");
    }
}
