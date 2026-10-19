package com.example.csvprocessor;

import com.example.csvprocessor.service.CsvReadResult;
import com.example.csvprocessor.service.CsvReaderService;
import com.example.csvprocessor.service.CsvWriterService;
import com.example.csvprocessor.service.EmployeeProcessingService;
import com.example.csvprocessor.service.ProcessingReport;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Runs the whole job at start-up: read the CSV, process it concurrently, write the result. */
@Component
public class ProcessorRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessorRunner.class);

    private final CsvReaderService reader;
    private final EmployeeProcessingService processor;
    private final CsvWriterService writer;
    private final Path inputFile;
    private final Path outputFile;

    public ProcessorRunner(
            CsvReaderService reader,
            EmployeeProcessingService processor,
            CsvWriterService writer,
            @Value("${app.input-file}") String inputFile,
            @Value("${app.output-file}") String outputFile) {
        this.reader = reader;
        this.processor = processor;
        this.writer = writer;
        this.inputFile = Path.of(inputFile);
        this.outputFile = Path.of(outputFile);
    }

    @Override
    public void run(String... args) {
        CsvReadResult input = reader.read(inputFile);
        ProcessingReport report = processor.process(input.employees());
        writer.write(outputFile, report.results());

        var stats = report.statistics();
        log.info("Done in {} ms: {} processed, {} increased, {} without increase, total increase {}",
                report.elapsed().toMillis(),
                stats.processed(),
                stats.increased(),
                stats.notEligible(),
                stats.totalIncrease().toPlainString());
        log.info("Peak number of tasks calculating at the same time: {}", stats.peakConcurrentTasks());
        if (!input.errors().isEmpty()) {
            log.warn("{} line(s) in the input file were skipped; see the warnings above",
                    input.errors().size());
        }
        log.info("Result file: {}", outputFile.toAbsolutePath());
    }
}
