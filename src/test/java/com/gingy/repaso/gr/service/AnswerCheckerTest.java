package com.gingy.repaso.gr.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerCheckerTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" -> {2}")
    @CsvSource(delimiter = '|', value = {
            "casa | casa | true",
            "CASA | casa | true",
            "  casa   | casa | true",
            "hogar | 'casa, hogar' | true",
            "casa | 'casa; hogar' | true",
            "hogar | 'casa / hogar' | true",
            "adios | adiós | true",
            "ano | año | false",
            "σπιτι | σπίτι | true",
            "ΣΠΊΤΙ | σπίτι | true",
            "οδος | οδός | true",
            "perro | gato | false",
            "'' | casa | false",
            "'   ' | casa | false"
    })
    void comparesAnswersToStoredTerms(String answer, String stored, boolean expected) {
        assertThat(AnswerChecker.isCorrect(answer, stored)).isEqualTo(expected);
    }

    @Test
    void emptyAnswerIsWrongForAnyTerm() {
        assertThat(AnswerChecker.isCorrect("", "")).isFalse();
        assertThat(AnswerChecker.isCorrect(null, "casa")).isFalse();
    }
}
