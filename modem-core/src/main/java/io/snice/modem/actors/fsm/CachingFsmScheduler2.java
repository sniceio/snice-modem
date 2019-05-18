package io.snice.modem.actors.fsm;

import io.hektor.core.ActorRef;
import io.hektor.fsm.Cancellable;
import io.hektor.fsm.Scheduler;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

public class CachingFsmScheduler2 implements Scheduler {

    private List<CancellableTask> tasks;
    private final io.hektor.core.Scheduler scheduler;
    private final ActorRef self;

    public CachingFsmScheduler2(final io.hektor.core.Scheduler scheduler, final ActorRef self) {
        this.scheduler = scheduler;
        this.self = self;
    }

    @Override
    public <T> Cancellable schedule(final Supplier<T> producer, final Duration delay) {
        final var event = producer.get(); // todo: this is wrong. Need to enhance hektor.io
        final var timeout = scheduler.schedule(event, self, self, delay);
        return new CancellableTask<T>(timeout);
    }

    public static class CancellableTask<T> implements Cancellable {

        private final io.hektor.core.Cancellable actualCancellable;

        public CancellableTask(final io.hektor.core.Cancellable actualCancellable) {
            this.actualCancellable = actualCancellable;
        }

        @Override
        public boolean cancel() {
            return actualCancellable.cancel();
        }
    }

}
