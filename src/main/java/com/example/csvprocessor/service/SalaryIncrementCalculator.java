package com.example.csvprocessor.service;

import com.example.csvprocessor.model.Employee;
import com.example.csvprocessor.model.SalaryResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.stereotype.Component;

/**
 * Applies the business rules for the salary increment. The class has no mutable state, so one
 * instance can be used by many threads at the same time.
 *
 * <p>Rules:
 * <ol>
 *   <li>An employee who completed less than {@value #MIN_COMPLETED_PERCENTAGE}% of their projects
 *       receives no increase at all.</li>
 *   <li>Otherwise the increase is the role percentage (Director 5%, Manager 2%, Employee 1%)
 *       plus 2% for every full year worked.</li>
 *   <li>The years worked are counted only once the employee has completed one full year. Before
 *       that, only the role percentage applies.</li>
 * </ol>
 * The percentages are added together and applied once to the current salary. The result is
 * rounded to two decimals (half up).
 */
@Component
public class SalaryIncrementCalculator {

    /** Minimum share of completed projects needed to qualify for an increase. */
    public static final int MIN_COMPLETED_PERCENTAGE = 60;

    /** Increase in percent for each full year worked. */
    public static final BigDecimal PERCENT_PER_YEAR = new BigDecimal("2");

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final Clock clock;

    public SalaryIncrementCalculator(Clock clock) {
        this.clock = clock;
    }

    /** Calculates the new salary for one employee. */
    public SalaryResult calculate(Employee employee) {
        BigDecimal salary = employee.salary();

        if (employee.completedProjectsPercentage() < MIN_COMPLETED_PERCENTAGE) {
            return new SalaryResult(
                    employee,
                    salary,
                    BigDecimal.ZERO.setScale(2),
                    BigDecimal.ZERO.setScale(2),
                    "No increase: completed projects below " + MIN_COMPLETED_PERCENTAGE + "%");
        }

        int years = fullYearsWorked(employee.joinedDate());
        BigDecimal rolePercent = employee.role().getIncrementPercent();
        BigDecimal tenurePercent =
                years >= 1 ? PERCENT_PER_YEAR.multiply(BigDecimal.valueOf(years)) : BigDecimal.ZERO;
        BigDecimal totalPercent = rolePercent.add(tenurePercent);

        BigDecimal increase =
                salary.multiply(totalPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);

        String reason = "Role " + employee.role() + " " + rolePercent.toPlainString() + "%"
                + (years >= 1
                        ? " + " + years + " year(s) worked x 2% = " + tenurePercent.toPlainString() + "%"
                        : " (less than one year worked, no tenure increase)")
                + " => " + totalPercent.toPlainString() + "%";

        return new SalaryResult(
                employee,
                salary.add(increase),
                totalPercent.setScale(2, RoundingMode.UNNECESSARY),
                increase,
                reason);
    }

    /** Number of complete years between the joined date and today. Future dates count as zero. */
    int fullYearsWorked(LocalDate joinedDate) {
        LocalDate today = LocalDate.now(clock);
        if (joinedDate.isAfter(today)) {
            return 0;
        }
        return Period.between(joinedDate, today).getYears();
    }
}
