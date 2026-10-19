package com.example.csvprocessor.model;

import java.math.BigDecimal;

/**
 * Outcome of applying the increment rules to one employee.
 *
 * @param employee          the original record
 * @param newSalary         salary after the increment (equal to the old salary if not eligible)
 * @param incrementPercent  total percentage applied (0 if not eligible)
 * @param increaseAmount    absolute increase in salary
 * @param reason            short human readable explanation of how the percentage was reached
 */
public record SalaryResult(
        Employee employee,
        BigDecimal newSalary,
        BigDecimal incrementPercent,
        BigDecimal increaseAmount,
        String reason) {

    /** True when the employee received a salary increase. */
    public boolean increased() {
        return incrementPercent.signum() > 0;
    }
}
