package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.DictionaryEntry;
import com.gingy.repaso.gr.repository.DictionaryEntryRepository;
import com.gingy.repaso.gr.service.TeiDictionaryParser.ParsedDictionary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Imports the FreeDict/WikDict dictionary and looks words up in it, in both directions.
 */
@Service
public class DictionaryService {

    private final DictionaryEntryRepository repository;
    private final TeiDictionaryParser parser;

    public DictionaryService(DictionaryEntryRepository repository, TeiDictionaryParser parser) {
        this.repository = repository;
        this.parser = parser;
    }

    /**
     * Replaces all rows of the same source with the contents of the TEI file.
     * Importing the same file again does not create duplicates.
     *
     * @return the number of pairs stored
     */
    @Transactional
    public int importTei(Path file) {
        ParsedDictionary parsed = parser.parse(file);
        repository.deleteBySource(parsed.source());
        repository.saveAll(parsed.entries());
        return parsed.entries().size();
    }

    /** Greek word in, its Spanish translations out. Ignores accents and case. */
    @Transactional(readOnly = true)
    public List<DictionaryEntry> findByGreek(String greek) {
        String key = TermNormalizer.normalize(greek);
        return key.isEmpty() ? List.of() : repository.findByGreekKeyOrderByIdAsc(key);
    }

    /** Spanish word in, its Greek translations out. Ignores accents and case, keeps ñ. */
    @Transactional(readOnly = true)
    public List<DictionaryEntry> findBySpanish(String spanish) {
        String key = TermNormalizer.normalize(spanish);
        return key.isEmpty() ? List.of() : repository.findBySpanishKeyOrderByIdAsc(key);
    }

    /** The dictionary pair matching both words (accents and case ignored), if it exists. */
    @Transactional(readOnly = true)
    public Optional<DictionaryEntry> findPair(String greek, String spanish) {
        String greekKey = TermNormalizer.normalize(greek);
        String spanishKey = TermNormalizer.normalize(spanish);
        if (greekKey.isEmpty() || spanishKey.isEmpty()) {
            return Optional.empty();
        }
        return repository.findFirstByGreekKeyAndSpanishKeyOrderByIdAsc(greekKey, spanishKey);
    }

    /**
     * Every pair that matches the word, whether it is Greek or Spanish. Duplicates are removed.
     * Empty if the word is blank.
     */
    @Transactional(readOnly = true)
    public List<DictionaryEntry> findMatches(String word) {
        Map<String, DictionaryEntry> pairs = new LinkedHashMap<>();
        for (DictionaryEntry e : findByGreek(word)) {
            pairs.putIfAbsent(e.getGreek() + "\u0000" + e.getSpanish(), e);
        }
        for (DictionaryEntry e : findBySpanish(word)) {
            pairs.putIfAbsent(e.getGreek() + "\u0000" + e.getSpanish(), e);
        }
        return new ArrayList<>(pairs.values());
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
