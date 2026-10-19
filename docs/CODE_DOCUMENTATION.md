# Code Documentation

Base package: `com.example.csvprocessor`

## Flow

```
ProcessorRunner
  1. CsvReaderService.read(path)                  -> CsvReadResult (employees + errors)
  2. EmployeeProcessingService.process(employees) -> ProcessingReport
        one task per employee on the thread pool, each calling SalaryIncrementCalculator
  3. CsvWriterService.write(path, results)
```

## model

### `Role` (enum)
| Member | Description |
|--------|-------------|
| `DIRECTOR`, `MANAGER`, `EMPLOYEE` | Roles with increase rates 5, 2 and 1 percent |
| `getIncrementPercent()` | Returns the role rate as a `BigDecimal` |
| `fromString(String)` | Case-insensitive parse. Throws `IllegalArgumentException` for empty or unknown text |

### `Employee` (record)
Immutable record: `rowNumber`, `name`, `salary` (`BigDecimal`), `joinedDate` (`LocalDate`), `role`,
`completedProjectsPercentage` (`double`). `rowNumber` is the line in the CSV file and is used to restore
the file order after parallel processing. Being immutable, an `Employee` is safe to share between threads.

### `SalaryResult` (record)
Outcome for one employee: `employee`, `newSalary`, `incrementPercent`, `increaseAmount`, `reason`.
`increased()` is true when the percentage is above zero.

## service

### `CsvReaderService`
| Method | Description |
|--------|-------------|
| `read(Path)` | Reads the file, skips the header, validates every line. Bad lines are collected as `"Line N: message"` and do not stop the run. Throws `UncheckedIOException` if the file cannot be read. |
| `splitLine(String)` (package-private) | Splits one CSV line; supports quoted fields and `""` as an escaped quote. |

Validation: 5 columns, non-empty name, salary a non-negative number (stored with 2 decimals),
date in `yyyy-MM-dd`, known role, percentage between 0 and 100 (NaN rejected).

### `CsvReadResult` (record)
`employees` and `errors`, both stored as unmodifiable lists.

### `SalaryIncrementCalculator`
Holds the business rules. It has no mutable state, so many threads can call it at once.

| Method | Description |
|--------|-------------|
| `calculate(Employee)` | Applies the rules and returns a `SalaryResult`. |
| `fullYearsWorked(LocalDate)` | Complete years between the joined date and today (from the injected `Clock`). A future date gives 0. |

Rules: under 60% completed -> no increase. Otherwise `role % + 2% x years`, where the tenure part applies
only when at least one full year has been worked. Money is `BigDecimal`, rounded to 2 decimals (half up).

### `EmployeeProcessingService`
| Method | Description |
|--------|-------------|
| `process(List<Employee>)` | Submits one task per employee to the `ExecutorService` with `invokeAll`, waits for all, rethrows any worker failure as `IllegalStateException`, sorts results back into file order and returns a `ProcessingReport`. |

Concurrency techniques:

| Technique | Where | Why |
|-----------|-------|-----|
| Thread pool (`ExecutorService`) | `ConcurrencyConfig.employeeExecutor` | Runs the per-employee tasks in parallel |
| `Semaphore` | `EmployeeProcessingService.permits` | Caps how many tasks calculate at once (`app.max-concurrent-tasks`) |
| `ReentrantLock` | `EmployeeProcessingService.processOne` | Protects the shared `ArrayList` of results, which is not thread-safe |
| Atomic variables | `ProcessingStatistics` | Lock-free counters (`AtomicInteger`, `AtomicLong`) |
| `synchronized` block | `ProgressTracker.taskCompleted` | Keeps the counter and the progress log line consistent |

### `ProcessingReport` (record)
`results` (in file order), `statistics` (`ProcessingStatistics.Snapshot`), `elapsed` (`Duration`).

### `CsvWriterService`
| Method | Description |
|--------|-------------|
| `write(Path, List<SalaryResult>)` | Writes header plus one line per result. Creates missing folders, replaces an existing file. Throws `UncheckedIOException` on failure. |
| `escape(String)` (package-private) | Quotes a field containing a comma, quote or line break. |

## concurrency

### `ProcessingStatistics`
Counters updated by worker threads: processed, increased, not eligible, total increase (kept as cents in an
`AtomicLong`) and the peak number of tasks running together. `snapshot()` returns an immutable
`Snapshot` record.

### `ProgressTracker`
Logs `Progress: n/total` every N completed tasks and at the end, from inside a `synchronized` block.

## config

### `ConcurrencyConfig`
| Bean | Description |
|------|-------------|
| `employeeExecutor(poolSize)` | Fixed thread pool with named threads (`employee-worker-N`), shut down by Spring when the context closes so the job can exit. |
| `clock(referenceDate)` | System clock, or a fixed clock when `app.reference-date` is set. |

## Application classes

* `CsvProcessorApplication` - Spring Boot `main`.
* `ProcessorRunner` - `CommandLineRunner` that runs the three steps and logs a summary.

## Tests (`src/test/java`)

| Class | Covers |
|-------|--------|
| `SalaryIncrementCalculatorTest` | 60% threshold (59.9 and 60), role rates, full-year counting (one day short), under one year, future date, rounding |
| `CsvReaderServiceTest` | Valid rows, quoted names, `%`, blank lines, every kind of bad line, empty and missing file |
| `EmployeeProcessingServiceTest` | Parallel result equals sequential result and keeps file order, exact statistics with 16 threads, semaphore cap, empty input, reuse |
| `CsvWriterServiceTest` | Output format, folder creation, escaping |
