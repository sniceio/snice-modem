package io.snice.processes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class ProcessTail implements Tail {

    private final Object lock = new Object(); // simple mutex
    private final ExecutorService threadPool;
    private final String cmd;
    private final Consumer<String> onNewLine;
    private final Consumer<String> onError;
    private final CompletableFuture<Void> exitStage = new CompletableFuture<>();
    private final Pattern regexp;
    private Process process;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);


    private ProcessTail(final String cmd, final ExecutorService threadPool, final Consumer<String> onNewLine, final Consumer<String> onError, final Pattern regexp) {
        this.cmd = cmd;
        this.threadPool = threadPool;
        this.onNewLine = onNewLine;
        this.onError = onError;
        this.regexp = regexp;
    }

    public static ProcessTailBuilder of(final String cmd, final ExecutorService threadPool) {
        assertNotEmpty(cmd, "The command cannot be null or the empty String");
        assertNotNull(threadPool, "The thread pool cannot be null");
        return new ProcessTailBuilder(threadPool, cmd);
    }

    @Override
    public CompletionStage<Void> start() {
        synchronized (lock) {
            try {
                final var builder = new ProcessBuilder();
                builder.command("/bin/sh", "-c", cmd);
                process = builder.start();
                final var output = new BufferedReader(new InputStreamReader(process.getInputStream()));
                final var error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                isRunning.set(true);
                threadPool.submit(new StreamReader(output, onNewLine, regexp));
                threadPool.submit(new StreamReader(error, onError, null));
                process.onExit().whenComplete((p, t) -> {
                    isRunning.set(false);
                    exitStage.complete(null);
                });

            } catch (final Throwable t) {
                isRunning.set(false);
                exitStage.completeExceptionally(t);
            }

            return exitStage;
        }
    }

    private class StreamReader implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(StreamReader.class);

        private final BufferedReader stream;
        private final Consumer<String> onData;
        private final Pattern regexp;
        private final Function<String, Boolean> matcher;

        StreamReader(final BufferedReader stream, final Consumer<String> onData, final Pattern regexp) {
            this.stream = stream;
            this.onData = onData;
            this.regexp = regexp;
            if (regexp == null) {
                matcher = s -> true;
            } else {
                matcher = s -> regexp.matcher(s).matches();
            }
        }

        @Override
        public void run() {
            // logger.info("Starting");
            while(isRunning.get()) {
                try {
                    final var line = stream.readLine();
                    if (line == null) {
                        // TODO: need a better strategy
                        Thread.sleep(100);
                    } else if (matcher.apply(line)) {
                        onData.accept(line);
                    }
                } catch (final Throwable t) {
                    // ignore
                }
            }
            // logger.info("Stopping");
        }
    }

    public static class ProcessTailBuilder implements Tail.Builder{

        private final ExecutorService threadPool;
        private final String cmd;
        private Consumer<String> onNewLine;
        private Consumer<String> onError;
        private Pattern regexp;

        private ProcessTailBuilder(final ExecutorService threadPool, final String cmd) {
            this.threadPool = threadPool;
            this.cmd = cmd;

        }

        @Override
        public Tail.Builder onNewLine(final Consumer<String> f) {
            assertNotNull(f, "The lambda for consuming new lines cannot be null");
            onNewLine = f;
            return this;
        }

        @Override
        public Tail.Builder onError(final Consumer<String> f) {
            assertNotNull(f, "The lambda for consuming error from stderror cannot be null");
            onError = f;
            return this;
        }

        @Override
        public Builder withFilter(final String regexp) throws PatternSyntaxException {
            assertNotEmpty(regexp, "The regular expression cannot be null or the empty String");
            this.regexp = Pattern.compile(regexp);
            return this;
        }

        @Override
        public Tail build() {
            return new ProcessTail(cmd, threadPool, onNewLine, onError, regexp);
        }
    }
}
