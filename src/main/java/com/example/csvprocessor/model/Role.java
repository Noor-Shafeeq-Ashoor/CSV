package com.example.csvprocessor.model;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Job roles supported by the processor, each with the salary increase (in percent) that the role
 * earns.
 */
public enum Role {

    DIRECTOR(new BigDecimal("5")),
    MANAGER(new BigDecimal("2")),
    EMPLOYEE(new BigDecimal("1"));

    private final BigDecimal incrementPercent;

    Role(BigDecimal incrementPercent) {
        this.incrementPercent = incrementPercent;
    }

    /** Returns the role based increase in percent (for example {@code 5} means 5%). */
    public BigDecimal getIncrementPercent() {
        return incrementPercent;
    }

    /**
     * Parses a role from CSV text. Matching is case-insensitive and ignores surrounding spaces.
     *
     * @throws IllegalArgumentException if the text is empty or not a known role
     */
    public static Role fromString(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Role is empty");
        }
        String trimmed = text.trim();
        try {
            return Role.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown role '" + trimmed + "' (expected Director, Manager or Employee)");
        }
    }
}
