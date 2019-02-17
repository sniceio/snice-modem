package io.snice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.hektor.config.HektorConfiguration;
import io.snice.old_modem.Modem;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class MShell implements Runnable {

    private final Modem modem;
    private final InputStream is;
    private final OutputStream os;
    private boolean isRunning = false;

    private Thread thread;

    private final CompletableFuture<MShell> shutdownFuture = new CompletableFuture<>();

    private static final List<String> exitCmds = Arrays.asList("exit", "quit");

    public MShell(final Modem modem, final InputStream is, final OutputStream os) {
        this.is = is;
        this.os = os;
        this.modem = modem;
    }

    public CompletionStage<MShell> start() {
        if (thread == null) {
            this.thread = new Thread(this);
            this.thread.start();
        }

        return shutdownFuture;
    }

    @Override
    public void run() {
        isRunning = true;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final PrintWriter writer = new PrintWriter(new OutputStreamWriter(os));
        sleep(100); // will go away once we fix the modem etc.
        writer.println("Welcome to LTE Shell");
        while (isRunning) {
            writer.write("> ");
            writer.flush();
            isRunning = processCmdLine(readLine(reader).orElse(""));
        }
        writer.println("Shutting down");
        writer.flush();
        shutdownFuture.complete(this);
    }

    private boolean processCmdLine(final String cmd) {
        if (cmd == null || cmd.isEmpty()) {
            return true;
        }

        if (isExit(cmd)) {
            return false;
        }

        return true;
    }

    private static boolean isExit(final String cmd) {
        return exitCmds.stream().filter(cmd::equalsIgnoreCase).findFirst().isPresent();
    }

    private static Optional<String> readLine(final BufferedReader reader) {
        try {
            return Optional.of(reader.readLine());
        } catch (final IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public static void main(final String... args) throws Exception {

        // final HektorConfiguration config = loadConfig("config.yml");
        // final Hektor hektor = Hektor.withName("hello").withConfiguration(config).build();
        // final ActorRef modemActor = hektor.actorOf(ModemActor.props(), "modem");
        // final ActorRef keyboard = hektor.actorOf(ConsoleActor.props(System.in, modemActor), "keyboard");
        // keyboard.tellAnonymously("hello world");
        // sleep(10000);

        final String port = "/dev/ttyUSB2";
        try {
            final Modem modem = Modem.of(port).open().toCompletableFuture().get(2000, TimeUnit.MILLISECONDS);
            if (args.length == 1 && "--reset".equals(args[0])) {
                System.out.println("Resetting modem");
                modem.stop();
                modem.stop().toCompletableFuture().get(1000, TimeUnit.MILLISECONDS);
                System.exit(1);
            }
            final MShell shell = new MShell(modem, System.in, System.out);
            shell.start().thenAccept(sh -> {
                try {
                    modem.stop().toCompletableFuture().get(1000, TimeUnit.MILLISECONDS);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
            System.err.println("Unable to connect to the modem. Exiting");
            System.err.println("You may want to try to run this as root with the command line argument --reset");
            System.err.println("(see log file for exception)");
            System.exit(1);
        }
    }

    private static HektorConfiguration loadConfig(final String file) throws IOException {
        final InputStream yamlStream = new FileInputStream(Paths.get(file).toFile());
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(yamlStream, HektorConfiguration.class);
    }

    private static void sleep(final int ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

}
