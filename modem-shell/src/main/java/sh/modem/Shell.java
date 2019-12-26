package sh.modem;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.hektor.config.HektorConfiguration;
import io.hektor.core.ActorRef;
import io.hektor.core.Hektor;
import io.hektor.core.Props;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.hektor.FsmActor;
import io.snice.hektor.OnStartFunction;
import io.snice.modem.fsm.ModemManagerContext;
import io.snice.modem.fsm.ModemManagerData;
import io.snice.modem.fsm.ModemManagerFsm;
import io.snice.modem.hektor.ActorModemManagerContext;
import io.snice.usb.event.Scan;
import io.snice.usb.fsm.UsbManagerContext;
import io.snice.usb.fsm.UsbManagerData;
import io.snice.usb.fsm.UsbManagerFsm;
import io.snice.usb.hektor.ActorUsbManagerContext;
import io.snice.usb.linux.LibUsbConfiguration;
import io.snice.usb.linux.LinuxLibUsbScanner;
import io.snice.usb.linux.UsbIdLoader;

import javax.usb.UsbException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

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

    private static Props configureUsbManager(final ExecutorService blockingIoPool, final LibUsbConfiguration usbConfiguration) throws UsbException {

        final var knownUsbVendors = UsbIdLoader.load();
        final var scanner = LinuxLibUsbScanner.of(usbConfiguration, knownUsbVendors);

        final Function<ActorRef, UsbManagerContext> usbManagerCtx
                = (ref) ->  new ActorUsbManagerContext(ref, scanner, usbConfiguration, knownUsbVendors);

        final UsbManagerData usbManagerData = new UsbManagerData();

        final OnStartFunction<UsbManagerContext, UsbManagerData> onStart = (actorCtx, ctx, data) -> {
            final var self = actorCtx.self();
            self.tell(Scan.SCAN);
        };

        return FsmActor.of(UsbManagerFsm.definition)
                .withContext(usbManagerCtx)
                .withData(usbManagerData)
                .withStartFunction(onStart)
                .build();
    }

    private static Props configureModemSubSystem(final ActorRef usbSubSystem) {

        final OnStartFunction<ModemManagerContext, ModemManagerData> onStart = (actorCtx, ctx, data) -> {
            final var self = actorCtx.self();
            self.tell(Scan.SCAN);
        };

        final Function<ActorRef, ModemManagerContext> ctx
                = (ref) ->  ActorModemManagerContext.withUsbSubSystem(usbSubSystem).withSelf(ref).build();

        final var data = new ModemManagerData();

        return FsmActor.of(ModemManagerFsm.definition)
                .withContext(ctx)
                .withData(data)
                .build();
    }

    public static void main(final String... args) throws Exception {

        // TODO: got to have a proper command line arg stuff.
        final ShellConfig shellConfig = loadConfig(args[0]);
        final HektorConfiguration hektorConfig = shellConfig.getHektorConfig();

        // TODO: should make it configurable.
        final ExecutorService blockingIoPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);

        final Hektor hektor = Hektor.withName("modem.sh").withConfiguration(hektorConfig).build();

        // final var props = FsmActor.of(UsbManagerFsm.definition).build();

        // final ActorRef usbManager = hektor.actorOf(UsbManagerActor.props(services, usbConfiguration), "usb");
        final var usbProps = configureUsbManager(blockingIoPool, shellConfig.getUsbConfiguration());
        final var usbSubSystem = hektor.actorOf(usbProps, "usb");

        final var modemProps = configureModemSubSystem(usbSubSystem);
        final var modemSubSystem = hektor.actorOf(modemProps, "modems");

        // final ActorRef modemManager = hektor.actorOf(ModemManagerActor.props(blockingIoPool), "modem_manager");
        // final ActorRef shell = hektor.actorOf(ShellActor.props(shellConfig, blockingIoPool, modemManager, System.in, System.out), "shell");
    }
}
