package io.snice;

import io.hektor.core.Actor;
import io.hektor.core.ActorRef;
import io.hektor.core.Props;

import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConsoleActor implements Actor, Runnable {

    public static Props props(final InputStream in, final ActorRef modem) {
        return Props.forActor(ConsoleActor.class, () -> new ConsoleActor(in, modem));
    }

    private final ActorRef self;
    private final ActorRef modem;
    private final Scanner scanner;

    final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ConsoleActor(final InputStream in, final ActorRef modem) {
        this.self = self();
        this.scanner = new Scanner(in);
        this.modem = modem;
    }

    @Override
    public void run() {
        final String line = scanner.nextLine();
        self.tell(line, self);
    }

    @Override
    public void onReceive(final Object msg) {
        if (sender() == self) {
            System.err.println("Got something from myself");
        }
        System.err.println("Ok so i got something: " + msg + " forwarding to modem");
        modem.tell(msg, self());

        System.err.println("> Say something");
        System.err.println("> ");
        executor.submit(this);
    }
}
