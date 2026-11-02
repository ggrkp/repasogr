package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.DictionaryEntry;
import com.gingy.repaso.gr.domain.ListItem;
import com.gingy.repaso.gr.domain.WordList;
import com.gingy.repaso.gr.repository.WordListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class WordListService {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final WordListRepository repository;
    private final DictionaryService dictionary;

    public WordListService(WordListRepository repository, DictionaryService dictionary) {
        this.repository = repository;
        this.dictionary = dictionary;
    }

    @Transactional(readOnly = true)
    public List<WordList> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    /** The list with its items loaded, or empty if there is no such list. */
    @Transactional(readOnly = true)
    public Optional<WordList> findWithItems(Long id) {
        return repository.findWithItemsById(id);
    }

    /** Dictionary pairs matching a Greek or Spanish word, for the user to choose from. */
    @Transactional(readOnly = true)
    public List<DictionaryEntry> findDictionaryMatches(String word) {
        return dictionary.findMatches(clean(word));
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

        boolean alreadyInList = list.getItems().stream()
                .anyMatch(i -> i.getGreek().equals(entry.getGreek()) && i.getSpanish().equals(entry.getSpanish()));
        if (alreadyInList) {
            throw new IllegalArgumentException("This word pair is already in the list.");
        }

        return list.addItem(entry.getGreek(), entry.getSpanish());
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
