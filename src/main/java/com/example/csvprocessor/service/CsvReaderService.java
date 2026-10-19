package com.example.csvprocessor.service;

import com.example.csvprocessor.model.Employee;
import com.example.csvprocessor.model.Role;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads employee records from a CSV file.
 *
 * <p>Expected columns (the first line is a header and is skipped):
 * {@code name,salary,joinedDate,role,completedProjectsPercentage}. Dates use {@code yyyy-MM-dd}.
 * Names that contain a comma may be wrapped in double quotes.
 *
 * <p>A bad line never stops the run: it is skipped and reported in {@link CsvReadResult#errors()}.
 */
@Service
public class CsvReaderService {

    private static final Logger log = LoggerFactory.getLogger(CsvReaderService.class);

    static final int EXPECTED_COLUMNS = 5;

    /**
     * Reads and validates the file.
     *
     * @throws UncheckedIOException if the file does not exist or cannot be read
     */
    public CsvReadResult read(Path file) {
        List<Employee> employees = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) {
                errors.add("File is empty");
                return new CsvReadResult(employees, errors);
            }

            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    employees.add(parseEmployee(lineNumber, line));
                } catch (IllegalArgumentException ex) {
                    errors.add("Line " + lineNumber + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read CSV file: " + file, ex);
        }

        log.info("Read {} valid employee record(s) and {} rejected line(s) from {}",
                employees.size(), errors.size(), file);
        errors.forEach(error -> log.warn("Skipped {}", error));
        return new CsvReadResult(employees, errors);
    }

    private Employee parseEmployee(int lineNumber, String line) {
        List<String> fields = splitLine(line);
        if (fields.size() != EXPECTED_COLUMNS) {
            throw new IllegalArgumentException(
                    "Expected " + EXPECTED_COLUMNS + " columns but found " + fields.size());
        }

        String name = fields.get(0).trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is empty");
        }

        BigDecimal salary = parseSalary(fields.get(1));
        LocalDate joinedDate = parseDate(fields.get(2));
        Role role = Role.fromString(fields.get(3));
        double completed = parsePercentage(fields.get(4));

        return new Employee(lineNumber, name, salary, joinedDate, role, completed);
    }

    private static BigDecimal parseSalary(String text) {
        String trimmed = text.trim();
        BigDecimal salary;
        try {
            salary = new BigDecimal(trimmed);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid salary '" + trimmed + "'");
        }
        if (salary.signum() < 0) {
            throw new IllegalArgumentException("Salary cannot be negative: " + trimmed);
        }
        return salary.setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate parseDate(String text) {
        String trimmed = text.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid joined date '" + trimmed + "' (expected yyyy-MM-dd)");
        }
    }

    private static double parsePercentage(String text) {
        String trimmed = text.trim();
        if (trimmed.endsWith("%")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        double value;
        try {
            value = Double.parseDouble(trimmed);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid completed projects percentage '" + text.trim() + "'");
        }
        // Written so that NaN is rejected as well.
        if (!(value >= 0 && value <= 100)) {
            throw new IllegalArgumentException(
                    "Completed projects percentage must be between 0 and 100: " + text.trim());
        }
        return value;
    }

    /**
     * Splits one CSV line into fields. Supports double-quoted fields and {@code ""} as an escaped
     * quote inside them.
     *
     * @throws IllegalArgumentException if a quoted field is not closed
     */
    static List<String> splitLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Unterminated quoted field");
        }
        fields.add(current.toString());
        return fields;
    }
}
