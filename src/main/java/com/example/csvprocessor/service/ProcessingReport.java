package com.example.csvprocessor.service;

import com.example.csvprocessor.concurrency.ProcessingStatistics;
import com.example.csvprocessor.model.SalaryResult;
import java.time.Duration;
import java.util.List;

/**
 * Everything produced by one processing run.
 *
 * @param results     one result per employee, in the same order as the CSV file
 * @param statistics  counters collected by the worker threads
 * @param elapsed     time spent processing
 */
public record ProcessingReport(
        List<SalaryResult> results, ProcessingStatistics.Snapshot statistics, Duration elapsed) {

    public ProcessingReport {
        results = List.copyOf(results);
    }
}
