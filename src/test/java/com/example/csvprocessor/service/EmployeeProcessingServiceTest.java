package com.example.csvprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.csvprocessor.model.Employee;
import com.example.csvprocessor.model.Role;
import com.example.csvprocessor.model.SalaryResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class EmployeeProcessingServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    private final SalaryIncrementCalculator calculator = new SalaryIncrementCalculator(
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    private static List<Employee> generateEmployees(int count) {
        Role[] roles = Role.values();
        List<Employee> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            employees.add(new Employee(
                    i + 2,
                    "Employee " + i,
                    BigDecimal.valueOf(1000 + (i % 50) * 37, 2).add(BigDecimal.valueOf(1000)),
                    TODAY.minusDays(i * 13L),
                    roles[i % roles.length],
                    (i * 7) % 101));
        }
        return employees;
    }

    private List<SalaryResult> runSequentially(List<Employee> employees) {
        return employees.stream().map(calculator::calculate).toList();
    }

    @Test
    void concurrentResultsMatchSequentialResultsAndKeepFileOrder() {
        List<Employee> employees = generateEmployees(2000);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            EmployeeProcessingService service =
                    new EmployeeProcessingService(executor, calculator, 4);

            ProcessingReport report = service.process(employees);

            assertEquals(runSequentially(employees), report.results());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void statisticsAreExactEvenWithManyThreads() {
        List<Employee> employees = generateEmployees(5000);
        List<SalaryResult> expected = runSequentially(employees);

        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            EmployeeProcessingService service =
                    new EmployeeProcessingService(executor, calculator, 16);

            var stats = service.process(employees).statistics();

            int expectedIncreased = (int) expected.stream().filter(SalaryResult::increased).count();
            BigDecimal expectedTotal = expected.stream()
                    .map(SalaryResult::increaseAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(5000, stats.processed());
            assertEquals(expectedIncreased, stats.increased());
            assertEquals(5000 - expectedIncreased, stats.notEligible());
            assertEquals(expectedTotal.toPlainString(), stats.totalIncrease().toPlainString());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void semaphoreLimitsParallelCalculations() {
        List<Employee> employees = generateEmployees(3000);
        ExecutorService executor = Executors.newFixedThreadPool(12);
        try {
            EmployeeProcessingService service =
                    new EmployeeProcessingService(executor, calculator, 2);

            var stats = service.process(employees).statistics();

            assertTrue(stats.peakConcurrentTasks() >= 1);
            assertTrue(stats.peakConcurrentTasks() <= 2,
                    "peak was " + stats.peakConcurrentTasks());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void emptyInputGivesEmptyReport() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            EmployeeProcessingService service =
                    new EmployeeProcessingService(executor, calculator, 2);

            ProcessingReport report = service.process(List.of());

            assertTrue(report.results().isEmpty());
            assertEquals(0, report.statistics().processed());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void serviceCanBeUsedMoreThanOnce() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            EmployeeProcessingService service =
                    new EmployeeProcessingService(executor, calculator, 4);

            ProcessingReport first = service.process(generateEmployees(100));
            ProcessingReport second = service.process(generateEmployees(100));

            assertEquals(first.results(), second.results());
            assertEquals(100, second.statistics().processed());
        } finally {
            executor.shutdown();
        }
    }
}
