package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.controller.TestSession;
import com.gingy.repaso.gr.domain.Direction;
import com.gingy.repaso.gr.domain.ListItem;
import com.gingy.repaso.gr.domain.WordList;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class TestService {

    /** A missed word, shown on the result page. */
    public record Missed(String prompt, String answer) {
    }

    /** A new test over all words of the list, in random order. */
    public TestSession start(WordList list, Direction direction) {
        List<Long> ids = list.getItems().stream().map(ListItem::getId).toList();
        return newSession(list.getId(), direction, ids);
    }

    /** A new test over the words missed in the previous test, in random order. */
    public TestSession retakeMissed(TestSession previous) {
        return newSession(previous.getListId(), previous.getDirection(), previous.getWrongItemIds());
    }

    public Optional<ListItem> findItem(WordList list, Long itemId) {
        return list.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst();
    }

    public String prompt(ListItem item, Direction direction) {
        return direction == Direction.EL_TO_ES ? item.getGreek() : item.getSpanish();
    }

    public String expected(ListItem item, Direction direction) {
        return direction == Direction.EL_TO_ES ? item.getSpanish() : item.getGreek();
    }

    public boolean check(ListItem item, Direction direction, String answer) {
        return AnswerChecker.isCorrect(answer, expected(item, direction));
    }

    public List<Missed> missedWords(WordList list, TestSession session) {
        List<Missed> missed = new ArrayList<>();
        for (Long id : session.getWrongItemIds()) {
            findItem(list, id).ifPresent(item -> missed.add(
                    new Missed(prompt(item, session.getDirection()), expected(item, session.getDirection()))));
        }
        return missed;
    }

    private static TestSession newSession(Long listId, Direction direction, List<Long> ids) {
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("This list has no words to test yet.");
        }
        List<Long> shuffled = new ArrayList<>(ids);
        Collections.shuffle(shuffled);
        return new TestSession(listId, direction, shuffled);
    }
}
