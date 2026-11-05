package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.OwnWord;
import com.gingy.repaso.gr.repository.OwnWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The user's own word pairs. Every custom translation added to a list is also saved here,
 * so it can be found again by search. Kept apart from the imported dictionary.
 */
@Service
public class OwnWordService {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final OwnWordRepository repository;

    public OwnWordService(OwnWordRepository repository) {
        this.repository = repository;
    }

    /** All saved pairs, sorted by Spanish then Greek. */
    @Transactional(readOnly = true)
    public List<OwnWord> findAll() {
        return repository.findAllByOrderBySpanishKeyAscGreekKeyAsc();
    }

    /**
     * Saved pairs whose Greek or Spanish text contains the query (accents and case ignored).
     * An empty query returns everything.
     */
    @Transactional(readOnly = true)
    public List<OwnWord> search(String query) {
        String key = TermNormalizer.normalize(clean(query)).replace("%", "").replace("_", "");
        return key.isEmpty() ? findAll() : repository.findContaining("%" + key + "%");
    }

    /**
     * Saves the pair if the user does not already have it (accents and case ignored).
     * Text is trimmed and whitespace collapsed; the first spelling saved is kept.
     *
     * @throws IllegalArgumentException if a word is blank
     */
    @Transactional
    public OwnWord saveIfAbsent(String rawGreek, String rawSpanish) {
        String greek = clean(rawGreek);
        String spanish = clean(rawSpanish);
        requireBothWords(greek, spanish);
        return repository.findFirstByGreekKeyAndSpanishKeyOrderByIdAsc(
                        TermNormalizer.normalize(greek), TermNormalizer.normalize(spanish))
                .orElseGet(() -> repository.save(new OwnWord(greek, spanish)));
    }

    /**
     * Changes a saved pair. Lists that copied the old text keep their own copy.
     *
     * @throws IllegalArgumentException if the word does not exist, a word is blank, or another saved pair is the same
     */
    @Transactional
    public OwnWord update(Long id, String rawGreek, String rawSpanish) {
        OwnWord word = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("That word is not in your words."));
        String greek = clean(rawGreek);
        String spanish = clean(rawSpanish);
        requireBothWords(greek, spanish);
        if (repository.existsByGreekKeyAndSpanishKeyAndIdNot(
                TermNormalizer.normalize(greek), TermNormalizer.normalize(spanish), id)) {
            throw new IllegalArgumentException("This word pair is already in your words.");
        }
        word.update(greek, spanish);
        return word;
    }

    /** Deletes a saved pair. Lists that copied it keep their own copy. */
    @Transactional
    public void delete(Long id) {
        OwnWord word = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("That word is not in your words."));
        repository.delete(word);
    }

    private static void requireBothWords(String greek, String spanish) {
        if (greek.isEmpty() || spanish.isEmpty()) {
            throw new IllegalArgumentException("Both the Greek and the Spanish word are required.");
        }
    }

    /** Saved pairs with a Greek or Spanish word matching the query exactly (accents and case ignored). */
    @Transactional(readOnly = true)
    public List<OwnWord> findMatches(String query) {
        String key = TermNormalizer.normalize(clean(query));
        return key.isEmpty() ? List.of() : repository.findByGreekKeyOrSpanishKeyOrderByIdAsc(key, key);
    }

    private static String clean(String raw) {
        return WHITESPACE.matcher(raw == null ? "" : raw.trim()).replaceAll(" ");
    }
}
