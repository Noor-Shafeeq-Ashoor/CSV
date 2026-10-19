# Project Report: Concurrent CSV Data Processor

## 1. Summary

The project is a Spring Boot application that reads employee records from a CSV file and calculates each
employee's salary increment concurrently using Java's Executor framework. Thread safety is ensured with a
`Semaphore`, a `ReentrantLock`, atomic variables and a `synchronized` block. The result is written to a new
CSV file together with a short explanation for every employee.

## 2. Requirements and how they were met

| Requirement | Implementation |
|-------------|----------------|
| Spring Boot project | Maven project on Spring Boot 3.3.5, Java 17, started as a command-line batch job |
| Read employee data from CSV | `CsvReaderService` - name, salary, joined date, role, completed projects % |
| Thread pooling with the Executor framework | `ExecutorService` fixed thread pool bean (`ConcurrencyConfig`), one task per employee submitted with `invokeAll` |
| Locks and semaphores | `Semaphore` limits parallel calculations; `ReentrantLock` guards the shared result list |
| Atomic variables / synchronized blocks | `ProcessingStatistics` (atomic counters), `ProgressTracker` (synchronized block) |
| Less than 60% completed -> no increase | `SalaryIncrementCalculator`, first rule checked |
| 2% per year worked, only after one full year | `SalaryIncrementCalculator.fullYearsWorked` |
| Director 5%, Manager 2%, Employee 1% | `Role` enum |
| Documentation | `README.md`, `docs/CODE_DOCUMENTATION.md`, Javadoc comments, this report |
| Git with at least 20 commits | See the git history in the project |
| Sample CSV | `data/employees.csv` (25 records covering the edge cases) |

## 3. Design

The application is a three-step pipeline run by `ProcessorRunner`: read, process, write.

* **Reading** is sequential. Invalid lines are skipped and reported rather than stopping the run.
* **Processing** is parallel. Each employee becomes one task. A task acquires a semaphore permit, calls the
  stateless `SalaryIncrementCalculator`, updates the atomic statistics, adds its result to the shared list
  under the lock, and releases the permit.
* **Ordering.** Threads finish in any order, so results are sorted by CSV line number before writing. The
  output therefore always has the same order as the input.
* **Writing** is sequential and happens after every task has finished.

Money is handled with `BigDecimal` (never `double`) and rounded to two decimals, half up. Records are
immutable, which removes the need for locking when they are shared between threads.

### Concurrency choices

| Technique | Purpose |
|-----------|---------|
| Fixed thread pool | Reuses a small number of threads instead of creating one per employee |
| Semaphore | Puts an upper limit on parallel work, configurable with `app.max-concurrent-tasks` |
| ReentrantLock | Makes adding to the shared `ArrayList` safe |
| Atomic variables | Counters need no lock; the total increase is kept in cents in an `AtomicLong` |
| Synchronized block | Keeps the progress counter and its log line together |

The lock is held only for the single `add` call, so threads spend almost no time waiting on it.

## 4. Assumptions

The task description leaves some points open. These interpretations were used:

1. **Percentages are added.** A qualifying employee receives `role % + 2% x full years`, applied once to the
   current salary (not compounded year by year).
2. **Role percentage always applies** to employees who completed at least 60% of their projects, including
   employees with less than one year of service. The "one full year" condition applies to the 2%-per-year part only.
3. **Exactly 60% qualifies.** Only "less than 60%" is excluded.
4. **Full years** are complete calendar years from the joined date to today. A joined date in the future counts as zero years.
5. The date used for "today" can be fixed with `app.reference-date` so results can be reproduced.

If the course expects a different reading (for example, no increase at all before one year of service),
the change is limited to `SalaryIncrementCalculator.calculate`.

## 5. Testing

JUnit 5 tests cover the calculator (including the boundary cases 59.9%, 60% and one day short of a year),
the CSV reader (good and bad input), the writer, and the concurrent service. The service tests check that
processing 2,000 employees on 8 threads gives exactly the same results, in the same order, as plain
sequential processing; that the statistics are exact with 5,000 employees on 16 threads; and that the
semaphore keeps the peak number of simultaneous calculations within its limit.

## 6. Results on the sample data

Run with `app.reference-date=2026-10-19`, 4 threads, 4 permits:

| Measure | Value |
|---------|-------|
| Employees processed | 25 |
| Received an increase | 20 |
| No increase (under 60% projects) | 5 |
| Total salary increase | 7,134.47 |

The sample file contains: a Director, Manager and Employee at several tenures; an employee at exactly 60%
and one at 59.9%; employees one day either side of a full year; two employees who joined within the last
year; one future joined date; and a quoted name containing a comma.

With only 25 small records the whole run takes a few milliseconds, so no speed-up from parallelism can be
shown on this data. The value of the design is that the same code scales to large files, and that the
tests show it gives results identical to sequential processing.

## 7. Limitations and possible improvements

* The whole file is read into memory before processing. Very large files could be streamed in batches.
* The CSV parser is intentionally small (quoted fields and escaped quotes, but no multi-line fields).
  A library such as OpenCSV could replace it.
* The salary calculation is very fast, so the thread pool matters most when per-record work is heavier
  (for example, database or network calls).

## 8. Conclusion

The application meets the stated requirements: it reads the CSV file, applies the project, tenure and role
rules concurrently with a thread pool and several concurrency-control mechanisms, and produces a verified,
ordered output file.
