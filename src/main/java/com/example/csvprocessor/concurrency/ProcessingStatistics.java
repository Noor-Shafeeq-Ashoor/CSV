package com.example.csvprocessor.concurrency;

import com.example.csvprocessor.model.SalaryResult;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters shared by all worker threads. Every counter is an atomic variable, so threads can
 * update them without taking a lock and without losing updates.
 *
 * <p>The total increase is kept as a whole number of cents in an {@link AtomicLong}, because
 * {@code BigDecimal} cannot be updated atomically.
 */
public final class ProcessingStatistics {

    private final AtomicInteger processed = new AtomicInteger();
    private final AtomicInteger increased = new AtomicInteger();
    private final AtomicInteger notEligible = new AtomicInteger();
    private final AtomicLong totalIncreaseCents = new AtomicLong();
    private final AtomicInteger activeTasks = new AtomicInteger();
    private final AtomicInteger peakActiveTasks = new AtomicInteger();

    /** Called when a task starts calculating. Also tracks the highest number of parallel tasks. */
    public void taskStarted() {
        int active = activeTasks.incrementAndGet();
        peakActiveTasks.accumulateAndGet(active, Math::max);
    }

    /** Called when a task has finished calculating. */
    public void taskFinished() {
        activeTasks.decrementAndGet();
    }

    /** Records the outcome of one employee. */
    public void recordResult(SalaryResult result) {
        processed.incrementAndGet();
        if (result.increased()) {
            increased.incrementAndGet();
            totalIncreaseCents.addAndGet(result.increaseAmount().movePointRight(2).longValueExact());
        } else {
            notEligible.incrementAndGet();
        }
    }

    /** Returns an immutable copy of the current values. */
    public Snapshot snapshot() {
        return new Snapshot(
                processed.get(),
                increased.get(),
                notEligible.get(),
                BigDecimal.valueOf(totalIncreaseCents.get(), 2),
                peakActiveTasks.get());
    }

    /**
     * Immutable view of the statistics.
     *
     * @param processed            employees processed
     * @param increased            employees who received an increase
     * @param notEligible          employees who received no increase
     * @param totalIncrease        sum of all salary increases
     * @param peakConcurrentTasks  highest number of tasks that were calculating at the same time
     */
    public record Snapshot(
            int processed,
            int increased,
            int notEligible,
            BigDecimal totalIncrease,
            int peakConcurrentTasks) {}
}
