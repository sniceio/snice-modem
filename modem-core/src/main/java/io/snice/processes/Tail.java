package io.snice.processes;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;

public interface Tail {

    static ExecutorServiceStep tailProcess(final String cmd) {
        return (threadPool) -> ProcessTail.of(cmd, threadPool);
    }

    interface ExecutorServiceStep {
        Builder withThreadPool(ExecutorService executorService);
    }

    /**
     * Start the tail operation, whether this is just tailing a regular old file or
     * if this tails the output of a process.
     *
     * @return a {@link CompletionStage} that when completing, signifies the exit of the {@link Tail} command. Hence,
     * you can chain this {@link CompletionStage} with other operations if you wish to know when the {@link Tail} ends.
     */
    CompletionStage<Void> start();

    interface Builder {

        /**
         * Whenever a new line is detected, this function will be called.
         *
         * @param f
         * @return
         */
        Builder onNewLine(Consumer<String> f);

        /**
         * If any output from stderror is detected, this function will be called.
         *
         * @param f
         * @return
         */
        Builder onError(Consumer<String> f);

        Builder withFilter(String regexp) throws PatternSyntaxException;

        Tail build();

    }

}
