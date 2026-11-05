package com.gingy.repaso.gr.domain;

import com.gingy.repaso.gr.service.TermNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A word pair the user typed themselves. Kept apart from the imported dictionary so that the
 * user's own data can be exported on its own. Lists copy pairs from here; they do not link to it.
 */
@Entity
@Table(name = "own_word", uniqueConstraints = @UniqueConstraint(
        name = "uk_own_word_pair", columnNames = {"greekKey", "spanishKey"}))
public class OwnWord {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String greek;

    @Column(nullable = false)
    private String spanish;

    /** Normalized copies (accents and case ignored) used to detect duplicates. */
    @Column(nullable = false)
    private String greekKey;

    @Column(nullable = false)
    private String spanishKey;

    protected OwnWord() {
        // for JPA
    }

    public OwnWord(String greek, String spanish) {
        this.greek = greek;
        this.spanish = spanish;
        this.greekKey = TermNormalizer.normalize(greek);
        this.spanishKey = TermNormalizer.normalize(spanish);
    }

    /** Changes the saved pair and recomputes its normalized copies. */
    public void update(String greek, String spanish) {
        this.greek = greek;
        this.spanish = spanish;
        this.greekKey = TermNormalizer.normalize(greek);
        this.spanishKey = TermNormalizer.normalize(spanish);
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
}
