package com.gingy.repaso.gr.domain;

import com.gingy.repaso.gr.service.TermNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One dictionary pair: a Greek headword and one Spanish translation of it.
 * Imported from FreeDict/WikDict. Lookups work in both directions: the Greek
 * column is searched for Greek input, the Spanish column for Spanish input.
 *
 * greekKey / spanishKey are normalized copies (lower case, accents removed,
 * except ñ) so that lookups ignore missing tonos and accents.
 */
@Entity
@Table(name = "dictionary_entry", indexes = {
        @Index(name = "idx_dictionary_greek_key", columnList = "greekKey"),
        @Index(name = "idx_dictionary_spanish_key", columnList = "spanishKey")
})
public class DictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** Where the pair came from, e.g. "FreeDict+WikDict ell-spa 2023.05.29". Used to replace a previous import. */
    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String greek;

    @Column(nullable = false)
    private String spanish;

    @Column(nullable = false)
    private String greekKey;

    @Column(nullable = false)
    private String spanishKey;

    protected DictionaryEntry() {
        // for JPA
    }

    public DictionaryEntry(String source, String greek, String spanish) {
        this.source = source;
        this.greek = greek;
        this.spanish = spanish;
        this.greekKey = TermNormalizer.normalize(greek);
        this.spanishKey = TermNormalizer.normalize(spanish);
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getGreek() {
        return greek;
    }

    public String getSpanish() {
        return spanish;
    }

    public String getGreekKey() {
        return greekKey;
    }

    public String getSpanishKey() {
        return spanishKey;
    }
}
