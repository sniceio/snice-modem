package io.snice.processes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

public interface Processes {

    /**
     * <p>
     * Execute a command and have any output it generates returned once the process finish.
     * You only want to use this one when you know that the process will return in a reasonable
     * amount of time and it doesn't spit out too much output. Only you can determine what "too much"
     * really means but anything the process writes to <code>stdout</code> must be buffered, which will
     * of course consume memory.
     * </p>
     *
     * <p>
     *     Note: this is just a convenience method for using {@link Tail#tailProcess(String)}.
     * </p>
     *
     *
     * @param cmd
     * @return
     */
    static CompletionStage<List<String>> execute(final String cmd) {
        final var future = new CompletableFuture<List<String>>();
        final var buffer = new ArrayList<String>();
        final var tail = Tail.tailProcess(cmd).withThreadPool(ForkJoinPool.commonPool()).onNewLine(buffer::add).build();
        final var tailFuture = tail.start();
        tailFuture.thenAccept(aVoid -> future.complete(buffer));
        tailFuture.exceptionally(e-> {
            future.completeExceptionally(e);
            return null;
        });
        return future;
    }
}
