package com.example.csvprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.csvprocessor.model.Employee;
import com.example.csvprocessor.model.Role;
import com.example.csvprocessor.model.SalaryResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SalaryIncrementCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    private final SalaryIncrementCalculator calculator = new SalaryIncrementCalculator(
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    private static Employee employee(String salary, LocalDate joined, Role role, double completed) {
        return new Employee(2, "Test Person", new BigDecimal(salary), joined, role, completed);
    }

    @Test
    void noIncreaseWhenLessThan60PercentOfProjectsCompleted() {
        SalaryResult result = calculator.calculate(
                employee("2000.00", TODAY.minusYears(10), Role.DIRECTOR, 59.9));

        assertFalse(result.increased());
        assertEquals("2000.00", result.newSalary().toPlainString());
        assertEquals("0.00", result.incrementPercent().toPlainString());
        assertEquals("0.00", result.increaseAmount().toPlainString());
    }

    @Test
    void exactly60PercentStillQualifies() {
        SalaryResult result = calculator.calculate(
                employee("1000.00", TODAY.minusYears(1), Role.EMPLOYEE, 60));

        assertTrue(result.increased());
        assertEquals("3.00", result.incrementPercent().toPlainString()); // 1% role + 2% for 1 year
        assertEquals("1030.00", result.newSalary().toPlainString());
    }

    @Test
    void directorWithThreeYearsGets5PercentPlus6Percent() {
        SalaryResult result = calculator.calculate(
                employee("10000.00", LocalDate.of(2023, 10, 19), Role.DIRECTOR, 90));

        assertEquals("11.00", result.incrementPercent().toPlainString());
        assertEquals("11100.00", result.newSalary().toPlainString());
        assertEquals("1100.00", result.increaseAmount().toPlainString());
    }

    @Test
    void oneDayShortOfAFullYearDoesNotCount() {
        // Joined 2023-10-20 means 2 full years on 2026-10-19, not 3.
        SalaryResult result = calculator.calculate(
                employee("1000.00", LocalDate.of(2023, 10, 20), Role.EMPLOYEE, 80));

        assertEquals("5.00", result.incrementPercent().toPlainString()); // 1% + 2 x 2%
    }

    @Test
    void lessThanOneYearGetsOnlyTheRolePercentage() {
        SalaryResult result = calculator.calculate(
                employee("1000.00", TODAY.minusMonths(6), Role.MANAGER, 80));

        assertEquals("2.00", result.incrementPercent().toPlainString());
        assertEquals("1020.00", result.newSalary().toPlainString());
    }

    @Test
    void futureJoinedDateCountsAsNoYears() {
        SalaryResult result = calculator.calculate(
                employee("1000.00", TODAY.plusMonths(3), Role.EMPLOYEE, 80));

        assertEquals("1.00", result.incrementPercent().toPlainString());
    }

    @Test
    void managerWithTwoYears() {
        SalaryResult result = calculator.calculate(
                employee("2000.00", TODAY.minusYears(2), Role.MANAGER, 70));

        assertEquals("6.00", result.incrementPercent().toPlainString()); // 2% + 2 x 2%
        assertEquals("2120.00", result.newSalary().toPlainString());
    }

    @Test
    void roundsToTwoDecimalsHalfUp() {
        // 3% of 1234.56 = 37.0368 -> 37.04
        SalaryResult result = calculator.calculate(
                employee("1234.56", TODAY.minusYears(1), Role.EMPLOYEE, 75));

        assertEquals("37.04", result.increaseAmount().toPlainString());
        assertEquals("1271.60", result.newSalary().toPlainString());
    }

    @Test
    void roleRatesMatchTheSpecification() {
        assertEquals(0, new BigDecimal("5").compareTo(Role.DIRECTOR.getIncrementPercent()));
        assertEquals(0, new BigDecimal("2").compareTo(Role.MANAGER.getIncrementPercent()));
        assertEquals(0, new BigDecimal("1").compareTo(Role.EMPLOYEE.getIncrementPercent()));
    }
}
