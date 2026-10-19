package com.example.csvprocessor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.csvprocessor.model.Employee;
import com.example.csvprocessor.model.Role;
import com.example.csvprocessor.model.SalaryResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvWriterServiceTest {

    private final CsvWriterService writer = new CsvWriterService();

    private static SalaryResult result(String name) {
        Employee employee = new Employee(
                2, name, new BigDecimal("1000.00"), LocalDate.of(2020, 1, 1), Role.EMPLOYEE, 80);
        return new SalaryResult(
                employee,
                new BigDecimal("1050.00"),
                new BigDecimal("5.00"),
                new BigDecimal("50.00"),
                "Role EMPLOYEE 1%, tenure 4%");
    }

    @Test
    void writesHeaderAndOneLinePerResult(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("out.csv");

        writer.write(file, List.of(result("Sara Ali")));

        List<String> lines = Files.readAllLines(file);
        assertEquals(CsvWriterService.HEADER, lines.get(0));
        assertEquals(
                "Sara Ali,2020-01-01,EMPLOYEE,80.0,1000.00,5.00,1050.00,\"Role EMPLOYEE 1%, tenure 4%\"",
                lines.get(1));
        assertEquals(2, lines.size());
    }

    @Test
    void createsMissingFolders(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("a/b/out.csv");

        writer.write(file, List.of());

        assertEquals(List.of(CsvWriterService.HEADER), Files.readAllLines(file));
    }

    @Test
    void escapesCommasAndQuotes() {
        assertEquals("\"Zayani, Hessa\"", CsvWriterService.escape("Zayani, Hessa"));
        assertEquals("\"say \"\"hi\"\"\"", CsvWriterService.escape("say \"hi\""));
        assertEquals("plain", CsvWriterService.escape("plain"));
    }
}
