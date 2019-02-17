package io.snice.modem.actors.fsm;

import io.hektor.fsm.Cancellable;
import io.hektor.fsm.Scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CachingFsmScheduler implements Scheduler {

    private List<CancellableTask> tasks;

    @Override
    public <T> Cancellable schedule(final Supplier<T> producer, final Duration delay) {
        final CancellableTask<T> task = new CancellableTask<T>(producer, delay);
        addTask(task);
        return task;
    }

    public List<CancellableTask> drainAllScheduledTasks() {
        final List<CancellableTask> list = tasks != null ? tasks : Collections.EMPTY_LIST;
        tasks = null;
        return list;
    }

    private void addTask(final CancellableTask task) {
        if (tasks == null) {
            tasks = new ArrayList<>(2);
        }

        tasks.add(task);
    }

    public static class CancellableTask<T> implements Cancellable {

        private boolean isCancelled;
        private final Supplier<T> producer;
        private final Duration delay;

        public CancellableTask(final Supplier<T> producer, final Duration delay) {
            this.producer = producer;
            this.delay = delay;
        }

        public Supplier<T> getProducer() {
            return producer;
        }

        public Duration getDelay() {
            return delay;
        }

        @Override
        public boolean cancel() {
            return isCancelled;
        }
    }

}
