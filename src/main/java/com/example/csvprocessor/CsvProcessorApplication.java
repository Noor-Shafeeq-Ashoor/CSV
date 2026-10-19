package com.example.csvprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Concurrent CSV Data Processor.
 *
 * <p>The application is a command-line batch job: it reads the employee CSV file, applies the
 * salary increment rules concurrently and writes the result to a new CSV file. It then exits.
 */
@SpringBootApplication
public class CsvProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsvProcessorApplication.class, args);
    }
}
