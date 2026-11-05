package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.domain.Direction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Progress of one test, kept in the HTTP session. Nothing is stored in the database. */
public class TestSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long listId;
    private final Direction direction;
    private final List<Long> itemIds;
    private final List<Long> wrongItemIds = new ArrayList<>();
    private int index;
    private boolean hintShown;

    /** @param itemIds the item ids in the order they will be asked */
    public TestSession(Long listId, Direction direction, List<Long> itemIds) {
        this.listId = listId;
        this.direction = direction;
        this.itemIds = new ArrayList<>(itemIds);
    }

    public Long getListId() {
        return listId;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isFinished() {
        return index >= itemIds.size();
    }

    /** Id of the item asked now. Only valid when the test is not finished. */
    public Long currentItemId() {
        return itemIds.get(index);
    }

    /** 1-based number of the current question. */
    public int position() {
        return index + 1;
    }

    public int total() {
        return itemIds.size();
    }

    public int correctCount() {
        return index - wrongItemIds.size();
    }

    public void record(Long itemId, boolean correct) {
        if (!correct) {
            wrongItemIds.add(itemId);
        }
        index++;
        hintShown = false;
    }

    /** Whether the user asked for a hint on the current question. Hints do not change the score. */
    public boolean isHintShown() {
        return hintShown;
    }

    public void showHint() {
        hintShown = true;
    }

    public List<Long> getWrongItemIds() {
        return List.copyOf(wrongItemIds);
    }
}
