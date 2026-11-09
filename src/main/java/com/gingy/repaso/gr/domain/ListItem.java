package com.gingy.repaso.gr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One word pair in a list. The text is copied from the dictionary entry it was added from,
 * or typed by the user.
 */
@Entity
@Table(name = "list_item")
public class ListItem {

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

    /** Changes the Greek and Spanish word of this pair. */
    public void update(String greek, String spanish) {
        this.greek = greek;
        this.spanish = spanish;
    }
}
