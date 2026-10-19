package com.example.csvprocessor.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One employee record read from the CSV file. Instances are immutable, so they can be shared
 * safely between threads.
 *
 * @param rowNumber                    line number in the CSV file (used to keep the output order)
 * @param name                         employee name
 * @param salary                       current salary
 * @param joinedDate                   date the employee joined the company
 * @param role                         role in the company
 * @param completedProjectsPercentage  percentage of projects completed, from 0 to 100
 */
public record Employee(
        int rowNumber,
        String name,
        BigDecimal salary,
        LocalDate joinedDate,
        Role role,
        double completedProjectsPercentage) {

    public Employee {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(salary, "salary");
        Objects.requireNonNull(joinedDate, "joinedDate");
        Objects.requireNonNull(role, "role");
    }
}
