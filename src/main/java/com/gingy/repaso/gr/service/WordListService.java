package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.DictionaryEntry;
import com.gingy.repaso.gr.domain.ListItem;
import com.gingy.repaso.gr.domain.OwnWord;
import com.gingy.repaso.gr.domain.WordList;
import com.gingy.repaso.gr.repository.WordListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class WordListService {

    /** A pair shown in search results. ownWord is true for pairs the user typed themselves. */
    public record WordMatch(String greek, String spanish, boolean ownWord) {
    }

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final WordListRepository repository;
    private final DictionaryService dictionary;
    private final OwnWordService ownWords;

    public WordListService(WordListRepository repository, DictionaryService dictionary, OwnWordService ownWords) {
        this.repository = repository;
        this.dictionary = dictionary;
        this.ownWords = ownWords;
    }

    @Transactional(readOnly = true)
    public List<WordList> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    /**
     * Lists whose name contains the query (accents and case ignored), sorted by name or by number of words.
     *
     * @param sort "words" for most words first; anything else sorts by name
     */
    @Transactional(readOnly = true)
    public List<WordList> findFiltered(String query, String sort) {
        String key = TermNormalizer.normalize(clean(query));
        Comparator<WordList> byName = Comparator.comparing(l -> TermNormalizer.normalize(l.getName()));
        Comparator<WordList> order = "words".equals(sort)
                ? Comparator.comparingInt((WordList l) -> l.getItems().size()).reversed().thenComparing(byName)
                : byName;
        return repository.findAllByOrderByNameAsc().stream()
                .filter(l -> key.isEmpty() || TermNormalizer.normalize(l.getName()).contains(key))
                .sorted(order)
                .toList();
    }

    /** The list with its items loaded, or empty if there is no such list. */
    @Transactional(readOnly = true)
    public Optional<WordList> findWithItems(Long id) {
        return repository.findWithItemsById(id);
    }

    /**
     * Pairs matching a Greek or Spanish word: the dictionary's pairs first, then the user's own saved pairs
     * that are not already in the dictionary results.
     */
    @Transactional(readOnly = true)
    public List<WordMatch> findDictionaryMatches(String word) {
        String query = clean(word);
        List<WordMatch> matches = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DictionaryEntry e : dictionary.findMatches(query)) {
            matches.add(new WordMatch(e.getGreek(), e.getSpanish(), false));
            seen.add(pairKey(e.getGreek(), e.getSpanish()));
        }
        for (OwnWord w : ownWords.findMatches(query)) {
            if (seen.add(pairKey(w.getGreek(), w.getSpanish()))) {
                matches.add(new WordMatch(w.getGreek(), w.getSpanish(), true));
            }
        }
        return matches;
    }

    private static String pairKey(String greek, String spanish) {
        return TermNormalizer.normalize(greek) + "\u0000" + TermNormalizer.normalize(spanish);
    }

    /**
     * Creates a list. The name is trimmed and repeated spaces are collapsed.
     *
     * @throws IllegalArgumentException if the name is blank or already used
     */
    @Transactional
    public WordList create(String rawName) {
        String name = clean(rawName);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("List name cannot be blank.");
        }
        if (repository.existsByName(name)) {
            throw new IllegalArgumentException("A list named \"" + name + "\" already exists.");
        }
        return repository.save(new WordList(name));
    }

    /**
     * Adds a word pair to a list. The pair must exist in the dictionary, and the list must not
     * already contain it. The stored text is the dictionary's spelling.
     *
     * @throws IllegalArgumentException if the list does not exist, a word is blank, the pair is
     *                                  not in the dictionary, or the list already has it
     */
    @Transactional
    public ListItem addItem(Long listId, String rawGreek, String rawSpanish) {
        WordList list = repository.findWithItemsById(listId)
                .orElseThrow(() -> new IllegalArgumentException("List not found."));

        String greek = clean(rawGreek);
        String spanish = clean(rawSpanish);
        if (greek.isEmpty() || spanish.isEmpty()) {
            throw new IllegalArgumentException("Both the Greek and the Spanish word are required.");
        }

        DictionaryEntry entry = dictionary.findPair(greek, spanish)
                .orElseThrow(() -> new IllegalArgumentException(
                        "\"" + greek + "\" = \"" + spanish + "\" is not in the dictionary."));

        requireNotInList(list, entry.getGreek(), entry.getSpanish());
        return list.addItem(entry.getGreek(), entry.getSpanish());
    }

    /**
     * Adds a word pair typed by the user, for when the dictionary has no such pair.
     * The text is stored as typed (trimmed and whitespace collapsed), and the pair is also
     * saved as one of the user's own words so that it can be found by search later.
     *
     * @throws IllegalArgumentException if the list does not exist, a word is blank, or the list already has the pair
     */
    @Transactional
    public ListItem addCustomItem(Long listId, String rawGreek, String rawSpanish) {
        WordList list = repository.findWithItemsById(listId)
                .orElseThrow(() -> new IllegalArgumentException("List not found."));

        String greek = clean(rawGreek);
        String spanish = clean(rawSpanish);
        if (greek.isEmpty() || spanish.isEmpty()) {
            throw new IllegalArgumentException("Both the Greek and the Spanish word are required.");
        }

        requireNotInList(list, greek, spanish);
        ListItem item = list.addItem(greek, spanish);
        ownWords.saveIfAbsent(greek, spanish); // remember the user's own word for search elsewhere
        return item;
    }

    /**
     * Changes the Greek and Spanish word of a pair in a list. The list's own copy changes only;
     * the dictionary and My words are not touched.
     *
     * @throws IllegalArgumentException if the list or word does not exist, a word is blank, or the list already has the pair
     */
    @Transactional
    public void updateItem(Long listId, Long itemId, String rawGreek, String rawSpanish) {
        WordList list = repository.findWithItemsById(listId)
                .orElseThrow(() -> new IllegalArgumentException("List not found."));
        ListItem item = list.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That word is not in this list."));
        String greek = clean(rawGreek);
        String spanish = clean(rawSpanish);
        if (greek.isEmpty() || spanish.isEmpty()) {
            throw new IllegalArgumentException("Both the Greek and the Spanish word are required.");
        }
        boolean clash = list.getItems().stream()
                .filter(i -> !i.getId().equals(itemId))
                .anyMatch(i -> samePair(i, greek, spanish));
        if (clash) {
            throw new IllegalArgumentException("This word pair is already in the list.");
        }
        item.update(greek, spanish);
    }

    /**
     * Adds an extra Greek translation to a word in a list (at most {@link ListItem#MAX_GREEK_TRANSLATIONS} per word).
     *
     * @throws IllegalArgumentException if the list or word does not exist, the text is blank, or the word is full
     */
    @Transactional
    public void addGreekTranslation(Long listId, Long itemId, String rawText) {
        ListItem item = findItem(listId, itemId);
        item.addGreekTranslation(clean(rawText));
    }

    /**
     * Removes the extra Greek translation at the given position (0-based) from a word in a list.
     *
     * @throws IllegalArgumentException if the list or word does not exist, or there is no such translation
     */
    @Transactional
    public void removeGreekTranslation(Long listId, Long itemId, int index) {
        ListItem item = findItem(listId, itemId);
        item.removeGreekTranslation(index);
    }

    private ListItem findItem(Long listId, Long itemId) {
        WordList list = repository.findWithItemsById(listId)
                .orElseThrow(() -> new IllegalArgumentException("List not found."));
        return list.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That word is not in this list."));
    }

    private static void requireNotInList(WordList list, String greek, String spanish) {
        boolean alreadyInList = list.getItems().stream()
                .anyMatch(i -> samePair(i, greek, spanish));
        if (alreadyInList) {
            throw new IllegalArgumentException("This word pair is already in the list.");
        }
    }

    /** Same pair ignoring case and accents, so "casa" and "Casá" count as one word. */
    private static boolean samePair(ListItem item, String greek, String spanish) {
        return TermNormalizer.normalize(item.getGreek()).equals(TermNormalizer.normalize(greek))
                && TermNormalizer.normalize(item.getSpanish()).equals(TermNormalizer.normalize(spanish));
    }

    /**
     * Removes one word from a list.
     *
     * @throws IllegalArgumentException if the list does not exist or the word is not in it
     */
    @Transactional
    public void removeItem(Long listId, Long itemId) {
        WordList list = repository.findWithItemsById(listId)
                .orElseThrow(() -> new IllegalArgumentException("List not found."));
        ListItem item = list.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That word is not in this list."));
        list.removeItem(item);
    }

    /**
     * Renames a list. Same rules as creating one: not blank, not used by another list.
     *
     * @throws IllegalArgumentException if the list does not exist, the name is blank, or another list has it
     */
    @Transactional
    public WordList rename(Long listId, String rawName) {
        WordList list = repository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("List not found."));
        String name = clean(rawName);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("List name cannot be blank.");
        }
        if (!name.equals(list.getName()) && repository.existsByName(name)) {
            throw new IllegalArgumentException("A list named \"" + name + "\" already exists.");
        }
        list.rename(name);
        return list;
    }

    /** Deletes a list and all its words. The dictionary is not changed. */
    @Transactional
    public void delete(Long listId) {
        WordList list = repository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("List not found."));
        repository.delete(list);
    }

    private static String clean(String raw) {
        return WHITESPACE.matcher(raw == null ? "" : raw.trim()).replaceAll(" ");
    }
}
