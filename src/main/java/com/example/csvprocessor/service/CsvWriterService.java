package com.example.csvprocessor.service;

import com.example.csvprocessor.model.Employee;
import com.example.csvprocessor.model.SalaryResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Writes the processed salaries to a CSV file. */
@Service
public class CsvWriterService {

    private static final Logger log = LoggerFactory.getLogger(CsvWriterService.class);

    static final String HEADER =
            "name,joinedDate,role,completedProjectsPercentage,oldSalary,incrementPercent,newSalary,reason";

    /**
     * Writes one line per result. Missing parent folders are created and an existing file is
     * replaced.
     *
     * @throws UncheckedIOException if the file cannot be written
     */
    public void write(Path file, List<SalaryResult> results) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(HEADER);
                writer.newLine();
                for (SalaryResult result : results) {
                    writer.write(toLine(result));
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot write CSV file: " + file, ex);
        }
        log.info("Wrote {} line(s) to {}", results.size(), file);
    }

    static String toLine(SalaryResult result) {
        Employee e = result.employee();
        return String.join(",",
                escape(e.name()),
                e.joinedDate().toString(),
                e.role().name(),
                String.valueOf(e.completedProjectsPercentage()),
                e.salary().toPlainString(),
                result.incrementPercent().toPlainString(),
                result.newSalary().toPlainString(),
                escape(result.reason()));
    }

    /** Wraps a field in quotes when it contains a comma, a quote or a line break. */
    static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
