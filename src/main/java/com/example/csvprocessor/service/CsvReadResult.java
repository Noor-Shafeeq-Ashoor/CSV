package com.example.csvprocessor.service;

import com.example.csvprocessor.model.Employee;
import java.util.List;

/**
 * Result of reading a CSV file: the valid employees plus one message for every rejected line.
 *
 * @param employees valid employee records, in file order
 * @param errors    messages such as {@code "Line 7: Unknown role 'Boss'"}
 */
public record CsvReadResult(List<Employee> employees, List<String> errors) {

    public CsvReadResult {
        employees = List.copyOf(employees);
        errors = List.copyOf(errors);
    }
}
