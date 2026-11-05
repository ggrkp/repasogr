package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.OwnWord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(OwnWordService.class)
class OwnWordServiceTest {

    @Autowired
    OwnWordService service;

    @Test
    void savesTrimmedPair() {
        OwnWord word = service.saveIfAbsent("  ψωμί ", " pan  ");

        assertThat(word.getId()).isNotNull();
        assertThat(word.getGreek()).isEqualTo("ψωμί");
        assertThat(word.getSpanish()).isEqualTo("pan");
    }

    @Test
    void samePairIgnoringAccentsAndCaseIsSavedOnce() {
        OwnWord first = service.saveIfAbsent("ψωμί", "pan");
        OwnWord again = service.saveIfAbsent("ΨΩΜΙ", "PAN");

        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(service.findMatches("pan")).hasSize(1);
    }

    @Test
    void rejectsBlankWords() {
        assertThatThrownBy(() -> service.saveIfAbsent("ψωμί", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both the Greek and the Spanish word are required.");
    }

    @Test
    void findsMatchesByGreekOrSpanish() {
        service.saveIfAbsent("ψωμί", "pan");
        service.saveIfAbsent("γάλα", "leche");

        assertThat(service.findMatches("ΨΩΜΙ")).extracting(OwnWord::getSpanish).containsExactly("pan");
        assertThat(service.findMatches("leche")).extracting(OwnWord::getGreek).containsExactly("γάλα");
        assertThat(service.findMatches("   ")).isEmpty();
    }

    @Test
    void searchMatchesPartOfGreekOrSpanishIgnoringAccents() {
        service.saveIfAbsent("ψωμί", "pan");
        service.saveIfAbsent("γάλα", "leche");

        assertThat(service.search("PA")).extracting(OwnWord::getSpanish).containsExactly("pan");
        assertThat(service.search("ψωμι")).extracting(OwnWord::getGreek).containsExactly("ψωμί");
        assertThat(service.search("%")).hasSize(2); // wildcard characters are stripped, so this is an empty query
        assertThat(service.search("")).hasSize(2);
    }

    @Test
    void updateChangesPairAndRejectsDuplicateOfAnotherWord() {
        OwnWord bread = service.saveIfAbsent("ψωμί", "pan");
        service.saveIfAbsent("γάλα", "leche");

        OwnWord updated = service.update(bread.getId(), "ψωμάκι", "panecito");
        assertThat(updated.getId()).isEqualTo(bread.getId());
        assertThat(service.search("panecito")).hasSize(1);

        assertThatThrownBy(() -> service.update(bread.getId(), "ΓΆΛΑ", "LECHE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This word pair is already in your words.");
    }

    @Test
    void updateKeepsSamePairWithoutError() {
        OwnWord bread = service.saveIfAbsent("ψωμί", "pan");

        assertThat(service.update(bread.getId(), "ψωμί", "pan ").getSpanish()).isEqualTo("pan");
    }

    @Test
    void updateUnknownWordIsRejected() {
        assertThatThrownBy(() -> service.update(424_242L, "ψωμί", "pan"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("That word is not in your words.");
    }

    @Test
    void deleteRemovesWord() {
        OwnWord word = service.saveIfAbsent("ψωμί", "pan");

        service.delete(word.getId());

        assertThat(service.findAll()).isEmpty();
        assertThatThrownBy(() -> service.delete(word.getId())).isInstanceOf(IllegalArgumentException.class);
    }
}
