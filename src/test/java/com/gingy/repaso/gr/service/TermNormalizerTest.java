package com.gingy.repaso.gr.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TermNormalizerTest {

    @Test
    void trimsCollapsesWhitespaceAndLowerCases() {
        assertThat(TermNormalizer.normalize("  Casa   Grande ")).isEqualTo("casa grande");
    }

    @Test
    void removesAccentsAndTonos() {
        assertThat(TermNormalizer.normalize("adiós")).isEqualTo("adios");
        assertThat(TermNormalizer.normalize("σπίτι")).isEqualTo("σπιτι");
        assertThat(TermNormalizer.normalize("οδός")).isEqualTo("οδοσ");
    }

    @Test
    void mapsFinalSigmaToSigma() {
        assertThat(TermNormalizer.normalize("οδος")).isEqualTo(TermNormalizer.normalize("οδός"));
        assertThat(TermNormalizer.normalize("ς")).isEqualTo("σ");
    }

    @Test
    void keepsTildeOnN() {
        assertThat(TermNormalizer.normalize("año")).isEqualTo("año");
        assertThat(TermNormalizer.normalize("año")).isNotEqualTo(TermNormalizer.normalize("ano"));
    }

    @Test
    void nullBecomesEmpty() {
        assertThat(TermNormalizer.normalize(null)).isEmpty();
    }
}
