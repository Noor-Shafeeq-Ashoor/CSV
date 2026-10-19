package com.example.csvprocessor.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs progress while worker threads finish their tasks.
 *
 * <p>A {@code synchronized} block protects the counter and the log line together, so progress
 * messages never appear out of order or with a repeated number.
 */
public final class ProgressTracker {

    private static final Logger log = LoggerFactory.getLogger(ProgressTracker.class);

    private final Object monitor = new Object();
    private final int total;
    private final int interval;
    private int completed;

    /**
     * @param total    number of tasks expected
     * @param interval a progress line is logged after every {@code interval} completed tasks
     */
    public ProgressTracker(int total, int interval) {
        this.total = total;
        this.interval = Math.max(1, interval);
    }

    /** Called by a worker thread when it has finished one task. */
    public void taskCompleted() {
        synchronized (monitor) {
            completed++;
            if (completed % interval == 0 || completed == total) {
                log.info("Progress: {}/{} employees processed", completed, total);
            }
        }
    }

    /** Number of tasks completed so far. */
    public int completed() {
        synchronized (monitor) {
            return completed;
        }
    }
}
