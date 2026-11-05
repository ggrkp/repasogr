package com.gingy.repaso.gr.domain;

import com.gingy.repaso.gr.service.TermNormalizer;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * One word pair in a list. The text is copied from the dictionary entry it was added from,
 * or typed by the user. A Spanish word can have up to {@link #MAX_GREEK_TRANSLATIONS} Greek
 * translations: the Greek word of the pair plus extra ones stored here.
 */
@Entity
@Table(name = "list_item")
public class ListItem {

    public static final int MAX_GREEK_TRANSLATIONS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private WordList list;

    @Column(nullable = false)
    private String greek;

    @Column(nullable = false)
    private String spanish;

    /** Greek translations beyond the one in {@link #greek}, in the order they were added. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_greek_translation", joinColumns = @JoinColumn(name = "item_id"))
    @OrderColumn(name = "position")
    @Column(name = "greek", nullable = false)
    private List<String> extraGreek = new ArrayList<>();

    protected ListItem() {
        // for JPA
    }

    ListItem(WordList list, String greek, String spanish) {
        this.list = list;
        this.greek = greek;
        this.spanish = spanish;
    }

    public Long getId() {
        return id;
    }

    public String getGreek() {
        return greek;
    }

    public String getSpanish() {
        return spanish;
    }

    /** Changes the Greek and Spanish word of this pair. Extra Greek translations are kept. */
    public void update(String greek, String spanish) {
        this.greek = greek;
        this.spanish = spanish;
    }

    /** All Greek translations of this word: the Greek word of the pair first, then the extra ones. */
    public List<String> getGreekTranslations() {
        List<String> all = new ArrayList<>();
        all.add(greek);
        all.addAll(extraGreek);
        return all;
    }

    /** The extra Greek translations only (not the Greek word of the pair). */
    public List<String> getExtraGreek() {
        return extraGreek;
    }

    /**
     * Adds an extra Greek translation.
     *
     * @throws IllegalArgumentException if the text is blank, contains a separator (, ; /),
     *                                  is already a translation of this word, or the word is full
     */
    public void addGreekTranslation(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("The Greek translation cannot be blank.");
        }
        if (text.matches(".*[,;/].*")) {
            throw new IllegalArgumentException("Enter one translation at a time (no commas, semicolons or slashes).");
        }
        if (getGreekTranslations().size() >= MAX_GREEK_TRANSLATIONS) {
            throw new IllegalArgumentException(
                    "Each Spanish word can have up to " + MAX_GREEK_TRANSLATIONS + " Greek translations.");
        }
        String key = TermNormalizer.normalize(text);
        boolean duplicate = getGreekTranslations().stream()
                .anyMatch(t -> TermNormalizer.normalize(t).equals(key));
        if (duplicate) {
            throw new IllegalArgumentException("That Greek translation is already listed for this word.");
        }
        extraGreek.add(text);
    }

    /**
     * Removes the extra Greek translation at the given position (0-based, in {@link #getExtraGreek()}).
     *
     * @throws IllegalArgumentException if there is no extra translation at that position
     */
    public void removeGreekTranslation(int index) {
        if (index < 0 || index >= extraGreek.size()) {
            throw new IllegalArgumentException("That translation does not exist.");
        }
        extraGreek.remove(index);
    }
}
