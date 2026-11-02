package com.gingy.repaso.gr.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** A named list of words the user wants to practise. Deleting a list deletes its items. */
@Entity
@Table(name = "word_list")
public class WordList {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ListItem> items = new ArrayList<>();

    protected WordList() {
        // for JPA
    }

    public WordList(String name) {
        this.name = name;
    }

    /** Adds a pair to this list. Validation is done by the service. */
    public ListItem addItem(String greek, String spanish) {
        ListItem item = new ListItem(this, greek, spanish);
        items.add(item);
        return item;
    }

    public void rename(String name) {
        this.name = name;
    }

    /** Removes an item from this list. The item row is deleted with it. */
    public void removeItem(ListItem item) {
        items.remove(item);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<ListItem> getItems() {
        return items;
    }
}
