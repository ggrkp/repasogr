package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.Direction;
import com.gingy.repaso.gr.domain.ListItem;
import com.gingy.repaso.gr.domain.WordList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestServiceTest {

    private final TestService testService = new TestService();

    private static ListItem casaWithExtraGreek() {
        ListItem item = new WordList("Casa").addItem("σπίτι", "casa");
        item.addGreekTranslation("οικία");
        return item;
    }

    @Test
    void spanishToGreekAcceptsAnyGreekTranslation() {
        ListItem item = casaWithExtraGreek();

        assertThat(testService.check(item, Direction.ES_TO_EL, "σπίτι")).isTrue();
        assertThat(testService.check(item, Direction.ES_TO_EL, "οικία")).isTrue();
        assertThat(testService.check(item, Direction.ES_TO_EL, "γάτα")).isFalse();
    }

    @Test
    void spanishToGreekShowsAllGreekTranslationsWhenWrong() {
        assertThat(testService.expected(casaWithExtraGreek(), Direction.ES_TO_EL)).isEqualTo("σπίτι, οικία");
    }

    @Test
    void greekToSpanishStillExpectsTheSpanishWord() {
        ListItem item = casaWithExtraGreek();

        assertThat(testService.expected(item, Direction.EL_TO_ES)).isEqualTo("casa");
        assertThat(testService.check(item, Direction.EL_TO_ES, "casa")).isTrue();
        assertThat(testService.check(item, Direction.EL_TO_ES, "οικία")).isFalse();
    }

    @Test
    void hintGivesFirstLetterOfEachAcceptedGreekAnswer() {
        assertThat(testService.hint(casaWithExtraGreek(), Direction.ES_TO_EL)).isEqualTo("σ / ο");
    }

    @Test
    void hintForSpanishAnswerIsItsFirstLetter() {
        assertThat(testService.hint(casaWithExtraGreek(), Direction.EL_TO_ES)).isEqualTo("c");
    }

    @Test
    void hintDistinguishesWordsWithTheSameSpanishTranslation() {
        ListItem house = new WordList("Casa").addItem("σπίτι", "casa");
        ListItem home = new WordList("Casa").addItem("οικία", "casa");

        assertThat(testService.hint(house, Direction.ES_TO_EL)).isEqualTo("σ");
        assertThat(testService.hint(home, Direction.ES_TO_EL)).isEqualTo("ο");
    }
}
