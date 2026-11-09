package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.Direction;
import com.gingy.repaso.gr.domain.ListItem;
import com.gingy.repaso.gr.domain.WordList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestServiceTest {

    private final TestService testService = new TestService();

    @Test
    void spanishToGreekAcceptsOnlyTheGreekWord() {
        ListItem item = new WordList("Casa").addItem("σπίτι", "casa");

        assertThat(testService.check(item, Direction.ES_TO_EL, "σπίτι")).isTrue();
        assertThat(testService.check(item, Direction.ES_TO_EL, "γάτα")).isFalse();
    }

    @Test
    void spanishToGreekShowsTheGreekWordWhenWrong() {
        ListItem item = new WordList("Casa").addItem("σπίτι", "casa");

        assertThat(testService.expected(item, Direction.ES_TO_EL)).isEqualTo("σπίτι");
    }

    @Test
    void greekToSpanishExpectsTheSpanishWord() {
        ListItem item = new WordList("Casa").addItem("σπίτι", "casa");

        assertThat(testService.expected(item, Direction.EL_TO_ES)).isEqualTo("casa");
        assertThat(testService.check(item, Direction.EL_TO_ES, "casa")).isTrue();
        assertThat(testService.check(item, Direction.EL_TO_ES, "σπίτι")).isFalse();
    }

    @Test
    void hintForGreekAnswerIsItsFirstLetter() {
        ListItem item = new WordList("Casa").addItem("σπίτι", "casa");

        assertThat(testService.hint(item, Direction.ES_TO_EL)).isEqualTo("σ");
    }

    @Test
    void hintForSpanishAnswerIsItsFirstLetter() {
        ListItem item = new WordList("Casa").addItem("σπίτι", "casa");

        assertThat(testService.hint(item, Direction.EL_TO_ES)).isEqualTo("c");
    }

    @Test
    void hintDistinguishesWordsWithTheSameSpanishTranslation() {
        ListItem house = new WordList("Casa").addItem("σπίτι", "casa");
        ListItem home = new WordList("Casa").addItem("οικία", "casa");

        assertThat(testService.hint(house, Direction.ES_TO_EL)).isEqualTo("σ");
        assertThat(testService.hint(home, Direction.ES_TO_EL)).isEqualTo("ο");
    }
}
