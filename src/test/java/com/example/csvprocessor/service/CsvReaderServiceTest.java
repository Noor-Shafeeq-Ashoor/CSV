package com.example.csvprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.csvprocessor.model.Employee;
import com.example.csvprocessor.model.Role;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvReaderServiceTest {

    private static final String HEADER = "name,salary,joinedDate,role,completedProjectsPercentage\n";

    private final CsvReaderService reader = new CsvReaderService();

    private static Path write(Path dir, String content) throws IOException {
        return Files.writeString(dir.resolve("employees.csv"), content);
    }

    @Test
    void readsValidRows(@TempDir Path dir) throws IOException {
        Path file = write(dir, HEADER
                + "Ahmed Ali,2800.5,2015-03-01,Director,92\n"
                + "Sara Nasser,1200,2020-01-10,employee,78.5\n");

        CsvReadResult result = reader.read(file);

        assertTrue(result.errors().isEmpty());
        assertEquals(2, result.employees().size());

        Employee first = result.employees().get(0);
        assertEquals(2, first.rowNumber());
        assertEquals("Ahmed Ali", first.name());
        assertEquals("2800.50", first.salary().toPlainString());
        assertEquals(LocalDate.of(2015, 3, 1), first.joinedDate());
        assertEquals(Role.DIRECTOR, first.role());
        assertEquals(92.0, first.completedProjectsPercentage(), 0.0001);

        Employee second = result.employees().get(1);
        assertEquals(Role.EMPLOYEE, second.role());
        assertEquals(78.5, second.completedProjectsPercentage(), 0.0001);
    }

    @Test
    void acceptsQuotedNamesAndPercentSign(@TempDir Path dir) throws IOException {
        Path file = write(dir, HEADER + "\"Zayani, Hessa\",1800,2016-04-25,Manager,72%\n");

        CsvReadResult result = reader.read(file);

        assertEquals(1, result.employees().size());
        assertEquals("Zayani, Hessa", result.employees().get(0).name());
        assertEquals(72.0, result.employees().get(0).completedProjectsPercentage(), 0.0001);
    }

    @Test
    void skipsBlankLinesButKeepsRealLineNumbers(@TempDir Path dir) throws IOException {
        Path file = write(dir, HEADER
                + "\n"
                + "Ahmed Ali,2800,2015-03-01,Director,92\n");

        CsvReadResult result = reader.read(file);

        assertEquals(1, result.employees().size());
        assertEquals(3, result.employees().get(0).rowNumber());
    }

    @Test
    void reportsEveryBadLineAndKeepsTheGoodOnes(@TempDir Path dir) throws IOException {
        Path file = write(dir, HEADER
                + "Good One,1000,2020-01-01,Employee,80\n"
                + "Bad Role,1000,2020-01-01,Boss,80\n"
                + "Bad Date,1000,01/02/2020,Employee,80\n"
                + "Bad Salary,abc,2020-01-01,Employee,80\n"
                + "Negative,-5,2020-01-01,Employee,80\n"
                + "Too High,1000,2020-01-01,Employee,101\n"
                + "Not A Number,1000,2020-01-01,Employee,lots\n"
                + "Missing Columns,1000,2020-01-01\n"
                + ",1000,2020-01-01,Employee,80\n"
                + "\"Unclosed,1000,2020-01-01,Employee,80\n");

        CsvReadResult result = reader.read(file);

        assertEquals(1, result.employees().size());
        assertEquals("Good One", result.employees().get(0).name());
        assertEquals(9, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Line 3:"));
        assertTrue(result.errors().get(0).contains("Unknown role"));
    }

    @Test
    void emptyFileIsReportedNotThrown(@TempDir Path dir) throws IOException {
        Path file = write(dir, "");

        CsvReadResult result = reader.read(file);

        assertTrue(result.employees().isEmpty());
        assertEquals(List.of("File is empty"), result.errors());
    }

    @Test
    void missingFileThrows(@TempDir Path dir) {
        Path missing = dir.resolve("does-not-exist.csv");

        assertThrows(UncheckedIOException.class, () -> reader.read(missing));
    }

    @Test
    void splitLineHandlesEscapedQuotes() {
        assertEquals(List.of("a", "say \"hi\"", "c"),
                CsvReaderService.splitLine("a,\"say \"\"hi\"\"\",c"));
    }

    @Test
    void splitLineKeepsEmptyFields() {
        assertEquals(List.of("a", "", "c", ""), CsvReaderService.splitLine("a,,c,"));
    }
}
