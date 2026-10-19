package com.example.csvprocessor.service;

import com.example.csvprocessor.concurrency.ProcessingStatistics;
import com.example.csvprocessor.concurrency.ProgressTracker;
import com.example.csvprocessor.model.Employee;
import com.example.csvprocessor.model.SalaryResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Processes employees in parallel on a thread pool.
 *
 * <p>Concurrency techniques used:
 * <ul>
 *   <li><b>Thread pool</b>: one task per employee is submitted to an {@link ExecutorService}.</li>
 *   <li><b>Semaphore</b>: limits how many tasks may calculate at the same time.</li>
 *   <li><b>ReentrantLock</b>: protects the shared result list, which is a plain
 *       {@link ArrayList} and is not thread-safe on its own.</li>
 *   <li><b>Atomic variables</b>: {@link ProcessingStatistics} counters are updated lock-free.</li>
 *   <li><b>Synchronized block</b>: {@link ProgressTracker} keeps progress messages consistent.</li>
 * </ul>
 * Threads finish in any order, so the results are sorted back into file order at the end.
 */
@Service
public class EmployeeProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeProcessingService.class);

    private static final int PROGRESS_INTERVAL = 10;

    private final ExecutorService executor;
    private final SalaryIncrementCalculator calculator;
    private final Semaphore permits;

    public EmployeeProcessingService(
            ExecutorService executor,
            SalaryIncrementCalculator calculator,
            @Value("${app.max-concurrent-tasks:4}") int maxConcurrentTasks) {
        if (maxConcurrentTasks < 1) {
            throw new IllegalArgumentException("app.max-concurrent-tasks must be at least 1");
        }
        this.executor = executor;
        this.calculator = calculator;
        this.permits = new Semaphore(maxConcurrentTasks);
    }

    /**
     * Applies the salary rules to every employee, using the thread pool, and waits until all tasks
     * have finished.
     *
     * @throws IllegalStateException if a task fails or the calling thread is interrupted
     */
    public ProcessingReport process(List<Employee> employees) {
        long startNanos = System.nanoTime();

        ProcessingStatistics statistics = new ProcessingStatistics();
        ProgressTracker progress = new ProgressTracker(employees.size(), PROGRESS_INTERVAL);
        ReentrantLock resultsLock = new ReentrantLock();
        List<SalaryResult> results = new ArrayList<>(employees.size());

        List<Callable<Void>> tasks = new ArrayList<>(employees.size());
        for (Employee employee : employees) {
            tasks.add(() -> {
                processOne(employee, statistics, resultsLock, results);
                progress.taskCompleted();
                return null;
            });
        }

        log.info("Submitting {} task(s) to the thread pool", tasks.size());
        try {
            List<Future<Void>> futures = executor.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get(); // rethrows any failure from a worker thread
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processing was interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("A processing task failed: " + ex.getCause(), ex.getCause());
        }

        results.sort(Comparator.comparingInt(result -> result.employee().rowNumber()));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        return new ProcessingReport(results, statistics.snapshot(), elapsed);
    }

    private void processOne(
            Employee employee,
            ProcessingStatistics statistics,
            ReentrantLock resultsLock,
            List<SalaryResult> results)
            throws InterruptedException {
        permits.acquire();
        try {
            statistics.taskStarted();
            try {
                SalaryResult result = calculator.calculate(employee);
                statistics.recordResult(result);

                resultsLock.lock();
                try {
                    results.add(result);
                } finally {
                    resultsLock.unlock();
                }
            } finally {
                statistics.taskFinished();
            }
        } finally {
            permits.release();
        }
    }
}
