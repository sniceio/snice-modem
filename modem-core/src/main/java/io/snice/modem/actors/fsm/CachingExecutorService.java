package io.snice.modem.actors.fsm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An {@link ExecutorService} that simply just caches all requests to run a job
 * so that the surrounding context (typically Hektor and it's runtime environment)
 * can schedule the real jobs after the invocation has occured successfully.
 */
public class CachingExecutorService implements ExecutorService {

    private List<CallableHolder> callables;

    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    public List<CallableHolder> removeAll() {
        final List<CallableHolder> list = callables;
        callables = null;
        return list;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        final CallableHolder holder = new CallableHolder(task);
        addCallable(holder);
        return holder.getFuture();
    }

    private void addCallable(final CallableHolder<?> callable) {
        if (callables == null) {
            callables = new ArrayList<>(2);
        }

        callables.add(callable);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public Future<?> submit(final Runnable task) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public void execute(final Runnable command) {
        throw new RuntimeException("not yet implemented");
    }

    public static class Holder<T> {
        private final CompletableFuture<T> future;
        private Holder() {
            future = new CompletableFuture<>();
        }

        public CompletableFuture<T> getFuture() {
            return future;
        }
    }

    public static class RunnableHolder<T> extends Holder<T> {
        private final Runnable task;
        private RunnableHolder(final Runnable task, final T result) {
            this.task = task;
        }
    }

    public static class CallableHolder<T> extends Holder<T> {
        private final Callable<T> task;

        private CallableHolder(final Callable<T> task) {
            this.task = task;
        }

        public Callable<T> getCallable() {
            return task;
        }
    }
}
