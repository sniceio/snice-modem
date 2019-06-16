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
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static void main(final String... args) throws Exception {

        // TODO: got to have a proper command line arg stuff.
        final ShellConfig shellConfig = loadConfig(args[0]);
        final HektorConfiguration hektorConfig = shellConfig.getHektorConfig();

        // TODO: should make it configurable.
        final ExecutorService blockingIoPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);

        final Hektor hektor = Hektor.withName("modem.sh").withConfiguration(hektorConfig).build();
        // final ActorRef modemManager = hektor.actorOf(ModemManagerActor.props(blockingIoPool), "modem_manager");
        // final ActorRef shell = hektor.actorOf(ShellActor.props(shellConfig, blockingIoPool, modemManager, System.in, System.out), "shell");
    }
}
