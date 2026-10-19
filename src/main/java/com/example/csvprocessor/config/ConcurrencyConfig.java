package com.example.csvprocessor.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the thread pool and the clock used by the processor. */
@Configuration
public class ConcurrencyConfig {

    /**
     * Fixed-size thread pool built with Java's Executor framework. Spring shuts it down when the
     * application closes, which lets the batch job exit.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService employeeExecutor(@Value("${app.thread-pool-size:4}") int poolSize) {
        if (poolSize < 1) {
            throw new IllegalArgumentException("app.thread-pool-size must be at least 1");
        }
        AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "employee-worker-" + counter.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        };
        return Executors.newFixedThreadPool(poolSize, factory);
    }

    /**
     * Clock used to count years worked. Set {@code app.reference-date} (yyyy-MM-dd) to get
     * repeatable results; leave it empty to use today's date.
     */
    @Bean
    public Clock clock(@Value("${app.reference-date:}") String referenceDate) {
        if (referenceDate == null || referenceDate.isBlank()) {
            return Clock.systemDefaultZone();
        }
        try {
            ZoneId zone = ZoneId.systemDefault();
            return Clock.fixed(
                    LocalDate.parse(referenceDate.trim()).atStartOfDay(zone).toInstant(), zone);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "app.reference-date must use the format yyyy-MM-dd: " + referenceDate, ex);
        }
    }
}
