package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.domain.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestSessionTest {

    @Test
    void hintIsForTheCurrentQuestionOnly() {
        TestSession session = new TestSession(1L, Direction.ES_TO_EL, List.of(10L, 11L));

        session.showHint();
        assertThat(session.isHintShown()).isTrue();

        session.record(10L, true);
        assertThat(session.isHintShown()).isFalse();
    }
}
