package com.gingy.repaso.gr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Imports the dictionary at startup when dictionary.import.tei is set, e.g.:
 * mvn spring-boot:run -Dspring-boot.run.arguments=--dictionary.import.tei=data/dictionaries/freedict-ell-spa-2023.05.29.tei
 * Without the property, nothing is imported.
 */
@Component
public class DictionaryImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DictionaryImportRunner.class);

    private final DictionaryService dictionaryService;
    private final String teiPath;

    public DictionaryImportRunner(DictionaryService dictionaryService,
                                  @Value("${dictionary.import.tei:}") String teiPath) {
        this.dictionaryService = dictionaryService;
        this.teiPath = teiPath;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (teiPath == null || teiPath.isBlank()) {
            return;
        }
        log.info("Importing dictionary from {}", teiPath);
        int count = dictionaryService.importTei(Path.of(teiPath));
        log.info("Imported {} dictionary pairs (total stored: {})", count, dictionaryService.count());
    }
}
