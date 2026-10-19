# Concurrent CSV Data Processor

A Spring Boot command-line application that reads employee records from a CSV file, applies a salary
increment to each employee **concurrently** on a thread pool, and writes the result to a new CSV file.

## Salary rules

| Rule | Behaviour |
|------|-----------|
| Projects completed | Less than 60% completed: **no increase at all**. 60% or more qualifies. |
| Role | Director **5%**, Manager **2%**, Employee **1%**. |
| Years worked | **2% for each full year** since the joined date, counted only once the employee has completed one full year. |

For a qualifying employee the percentages are added together and applied once to the current salary:

```
increase % = role %  +  (2% x full years worked)
new salary = salary + salary x increase % / 100        (rounded to 2 decimals, half up)
```

Example: a Director with 3 full years and 90% completed projects gets 5% + 6% = **11%**.

## Requirements

* Java 17 or newer
* Maven 3.9 or newer (internet access the first time, to download dependencies)

## Setup and run

```bash
# 1. Build and run the tests
mvn clean test

# 2. Run the application (reads data/employees.csv, writes output/employees_updated.csv)
mvn spring-boot:run

# Or build a jar and run it
mvn clean package
java -jar target/concurrent-csv-processor-1.0.0.jar
```

Any setting can be overridden on the command line:

```bash
java -jar target/concurrent-csv-processor-1.0.0.jar \
  --app.input-file=data/employees.csv \
  --app.output-file=output/result.csv \
  --app.thread-pool-size=8 \
  --app.max-concurrent-tasks=4 \
  --app.reference-date=2026-10-19
```

| Property | Default | Meaning |
|----------|---------|---------|
| `app.input-file` | `data/employees.csv` | CSV file to read |
| `app.output-file` | `output/employees_updated.csv` | CSV file to write (folders are created) |
| `app.thread-pool-size` | `4` | Worker threads in the pool |
| `app.max-concurrent-tasks` | `4` | Semaphore permits: tasks allowed to calculate at the same time |
| `app.reference-date` | empty (today) | Date used to count years worked, `yyyy-MM-dd`. Set it for repeatable results. |

The committed file `output/employees_updated.csv` was produced with `--app.reference-date=2026-10-19`.

## Input format

```
name,salary,joinedDate,role,completedProjectsPercentage
Ahmed Al Khalifa,2800.00,2015-03-01,Director,92
"Al Zayani, Hessa",1800.00,2016-04-25,Manager,72
```

* `joinedDate` uses `yyyy-MM-dd`. `role` is Director, Manager or Employee (any letter case).
* `completedProjectsPercentage` is 0 to 100 (a trailing `%` is accepted).
* Names containing a comma must be wrapped in double quotes.
* An invalid line is **skipped and reported as a warning**; the rest of the file is still processed.

## Output format

```
name,joinedDate,role,completedProjectsPercentage,oldSalary,incrementPercent,newSalary,reason
```

`reason` explains how the percentage was reached, or why no increase was given.

## Project layout

```
data/employees.csv                      sample input
output/employees_updated.csv            sample output
docs/CODE_DOCUMENTATION.md              class and method documentation
docs/PROJECT_REPORT.md                  project report
src/main/java/com/example/csvprocessor/
    CsvProcessorApplication.java        Spring Boot entry point
    ProcessorRunner.java                runs read -> process -> write
    config/ConcurrencyConfig.java       thread pool and clock beans
    model/                              Employee, Role, SalaryResult
    service/                            CSV reader/writer, calculator, processing service
    concurrency/                        ProcessingStatistics, ProgressTracker
src/test/java/...                       JUnit 5 tests
```

## Documentation

* [Code documentation](docs/CODE_DOCUMENTATION.md)
* [Project report](docs/PROJECT_REPORT.md)
