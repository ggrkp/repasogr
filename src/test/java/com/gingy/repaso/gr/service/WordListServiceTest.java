package com.gingy.repaso.gr.service;

import jakarta.persistence.EntityManager;
import com.gingy.repaso.gr.domain.ListItem;
import com.gingy.repaso.gr.domain.WordList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({WordListService.class, DictionaryService.class, TeiDictionaryParser.class, OwnWordService.class})
class WordListServiceTest {

    private static final String TEI = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TEI xmlns="http://www.tei-c.org/ns/1.0">
              <teiHeader><fileDesc><editionStmt><edition>test</edition></editionStmt></fileDesc></teiHeader>
              <text><body xml:lang="el">
                <entry>
                  <form><orth>σπίτι</orth></form>
                  <sense><cit type="trans" xml:lang="es"><quote>casa, hogar</quote></cit></sense>
                </entry>
                <entry>
                  <form><orth>γάτα</orth></form>
                  <sense><cit type="trans" xml:lang="es"><quote>gato</quote></cit></sense>
                </entry>
              </body></text>
            </TEI>
            """;

    @Autowired
    WordListService service;

    @Autowired
    DictionaryService dictionary;

    @Autowired
    EntityManager entityManager;

    @Autowired
    OwnWordService ownWords;

    @TempDir
    Path dir;

    @BeforeEach
    void importDictionary() throws Exception {
        Path file = dir.resolve("ell-spa.tei");
        Files.writeString(file, TEI);
        dictionary.importTei(file);
    }

    @Test
    void createsListWithTrimmedAndCollapsedName() {
        WordList list = service.create("  Verbos   irregulares ");

        assertThat(list.getId()).isNotNull();
        assertThat(list.getName()).isEqualTo("Verbos irregulares");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> service.create("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("List name cannot be blank.");
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateName() {
        service.create("Casa");

        assertThatThrownBy(() -> service.create("  Casa "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A list named \"Casa\" already exists.");
    }

    @Test
    void addsDictionaryPairUsingTheDictionarySpelling() {
        WordList list = service.create("Casas");

        ListItem item = service.addItem(list.getId(), "ΣΠΙΤΙ", "  Casa ");

        assertThat(item.getGreek()).isEqualTo("σπίτι");
        assertThat(item.getSpanish()).isEqualTo("casa");
        assertThat(service.findWithItems(list.getId()).orElseThrow().getItems()).hasSize(1);
    }

    @Test
    void rejectsPairThatIsNotInTheDictionary() {
        WordList list = service.create("Mixed");

        assertThatThrownBy(() -> service.addItem(list.getId(), "σπίτι", "perro"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("\"σπίτι\" = \"perro\" is not in the dictionary.");
        assertThat(service.findWithItems(list.getId()).orElseThrow().getItems()).isEmpty();
    }

    @Test
    void addsOwnPairThatIsNotInTheDictionary() {
        WordList list = service.create("Own");

        ListItem item = service.addCustomItem(list.getId(), "  ψωμί ", " pan ");

        assertThat(item.getGreek()).isEqualTo("ψωμί");
        assertThat(item.getSpanish()).isEqualTo("pan");
        assertThat(service.findWithItems(list.getId()).orElseThrow().getItems()).hasSize(1);
    }

    @Test
    void ownPairCannotBeAddedTwice() {
        WordList list = service.create("Own twice");
        service.addCustomItem(list.getId(), "ψωμί", "pan");

        assertThatThrownBy(() -> service.addCustomItem(list.getId(), "ψωμί", "pan"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This word pair is already in the list.");
    }

    @Test
    void pairDifferingOnlyInCaseOrAccentsIsTreatedAsDuplicate() {
        WordList list = service.create("Accents");
        service.addCustomItem(list.getId(), "ψωμί", "pan");

        assertThatThrownBy(() -> service.addCustomItem(list.getId(), "ΨΩΜΙ", "Pan"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This word pair is already in the list.");
        service.addItem(list.getId(), "σπίτι", "hogar");
        assertThatThrownBy(() -> service.addItem(list.getId(), "σπιτι", "Hogar"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This word pair is already in the list.");
    }

    @Test
    void editingIntoAnAccentVariantOfAnotherWordIsRejected() {
        WordList list = service.create("Edit accents");
        service.addCustomItem(list.getId(), "ψωμί", "pan");
        ListItem other = service.addCustomItem(list.getId(), "γάτα", "gato");

        assertThatThrownBy(() -> service.updateItem(list.getId(), other.getId(), "ΨΩΜΙ", "PAN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This word pair is already in the list.");
    }

    @Test
    void ownPairNeedsBothWords() {
        WordList list = service.create("Own blank");

        assertThatThrownBy(() -> service.addCustomItem(list.getId(), "ψωμί", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both the Greek and the Spanish word are required.");
    }

    @Test
    void addsExtraGreekTranslationsUpToThreeInTotal() {
        WordList list = service.create("Translations");
        ListItem item = service.addItem(list.getId(), "σπίτι", "casa");
        entityManager.flush(); // assign the id, as a committed request would have

        service.addGreekTranslation(list.getId(), item.getId(), " οικία ");
        service.addGreekTranslation(list.getId(), item.getId(), "σπιτάκι");

        ListItem stored = service.findWithItems(list.getId()).orElseThrow().getItems().get(0);
        assertThat(stored.getGreekTranslations()).containsExactly("σπίτι", "οικία", "σπιτάκι");
        assertThat(stored.getExtraGreek()).containsExactly("οικία", "σπιτάκι");
    }

    @Test
    void fourthGreekTranslationIsRejected() {
        WordList list = service.create("Four");
        ListItem item = service.addItem(list.getId(), "σπίτι", "casa");
        entityManager.flush();
        service.addGreekTranslation(list.getId(), item.getId(), "οικία");
        service.addGreekTranslation(list.getId(), item.getId(), "σπιτάκι");

        assertThatThrownBy(() -> service.addGreekTranslation(list.getId(), item.getId(), "τέσσερα"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Each Spanish word can have up to 3 Greek translations.");
    }

    @Test
    void duplicateGreekTranslationIsRejected() {
        WordList list = service.create("Dup translation");
        ListItem item = service.addItem(list.getId(), "σπίτι", "casa");
        entityManager.flush();

        assertThatThrownBy(() -> service.addGreekTranslation(list.getId(), item.getId(), "ΣΠΙΤΙ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("That Greek translation is already listed for this word.");
    }

    @Test
    void blankGreekTranslationIsRejected() {
        WordList list = service.create("Blank translation");
        ListItem item = service.addItem(list.getId(), "σπίτι", "casa");
        entityManager.flush();

        assertThatThrownBy(() -> service.addGreekTranslation(list.getId(), item.getId(), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The Greek translation cannot be blank.");
    }

    @Test
    void removesExtraGreekTranslationByPosition() {
        WordList list = service.create("Remove translation");
        ListItem item = service.addItem(list.getId(), "σπίτι", "casa");
        entityManager.flush();
        service.addGreekTranslation(list.getId(), item.getId(), "οικία");
        service.addGreekTranslation(list.getId(), item.getId(), "σπιτάκι");

        service.removeGreekTranslation(list.getId(), item.getId(), 0);

        ListItem stored = service.findWithItems(list.getId()).orElseThrow().getItems().get(0);
        assertThat(stored.getGreekTranslations()).containsExactly("σπίτι", "σπιτάκι");
    }

    @Test
    void greekTranslationOnWordNotInListIsRejected() {
        WordList list = service.create("No word");

        assertThatThrownBy(() -> service.addGreekTranslation(list.getId(), 424_242L, "ένα"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("That word is not in this list.");
    }

    @Test
    void customTranslationIsAlsoSavedAsOwnWordAndFoundElsewhere() {
        WordList first = service.create("Primera");
        WordList second = service.create("Segunda");

        service.addCustomItem(first.getId(), "ψωμί", "pan");

        assertThat(service.findWithItems(first.getId()).orElseThrow().getItems()).hasSize(1);
        assertThat(ownWords.findMatches("pan")).extracting(w -> w.getGreek()).containsExactly("ψωμί");
        assertThat(service.findDictionaryMatches("ψωμι"))
                .containsExactly(new WordListService.WordMatch("ψωμί", "pan", true));

        service.addCustomItem(second.getId(), "ψωμί", "pan"); // not a problem: a second list, same pair
        assertThat(service.findWithItems(second.getId()).orElseThrow().getItems()).hasSize(1);
        assertThat(ownWords.findMatches("pan")).hasSize(1);
    }

    @Test
    void searchListsDictionaryPairsBeforeOwnWordsWithoutDuplicates() {
        service.addCustomItem(service.create("Own").getId(), "σπίτι", "hogar");
        service.addCustomItem(service.create("Own2").getId(), "σπίτι", "vivienda");

        assertThat(service.findDictionaryMatches("σπίτι"))
                .containsExactly(
                        new WordListService.WordMatch("σπίτι", "casa", false),
                        new WordListService.WordMatch("σπίτι", "hogar", false),
                        new WordListService.WordMatch("σπίτι", "vivienda", true));
    }

    @Test
    void ownWordThatIsAlsoInDictionaryIsShownOnce() {
        service.addCustomItem(service.create("Dup").getId(), "σπίτι", "casa");

        assertThat(service.findDictionaryMatches("σπίτι"))
                .containsExactly(
                        new WordListService.WordMatch("σπίτι", "casa", false),
                        new WordListService.WordMatch("σπίτι", "hogar", false));
    }

    @Test
    void rejectsSameWordTwice() {
        WordList list = service.create("Twice");
        service.addItem(list.getId(), "σπίτι", "hogar");

        assertThatThrownBy(() -> service.addItem(list.getId(), "σπιτι", "hogar"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This word pair is already in the list.");
    }

    @Test
    void rejectsBlankWords() {
        WordList list = service.create("Blank");

        assertThatThrownBy(() -> service.addItem(list.getId(), " ", "casa"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.addItem(list.getId(), "σπίτι", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownList() {
        assertThatThrownBy(() -> service.addItem(9_999L, "σπίτι", "casa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("List not found.");
    }

    @Test
    void findAllReturnsListsSortedByName() {
        service.create("Zeta");
        service.create("Alfa");

        assertThat(service.findAll()).extracting(WordList::getName).containsExactly("Alfa", "Zeta");
    }

    @Test
    void removesWordFromList() {
        WordList list = service.create("Quitar");
        ListItem item = service.addItem(list.getId(), "σπίτι", "casa");
        entityManager.flush(); // assign the id, as a committed request would have

        service.removeItem(list.getId(), item.getId());

        assertThat(service.findWithItems(list.getId()).orElseThrow().getItems()).isEmpty();
    }

    @Test
    void removingWordNotInListIsRejected() {
        WordList list = service.create("Otra");

        assertThatThrownBy(() -> service.removeItem(list.getId(), 424_242L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("That word is not in this list.");
    }

    @Test
    void deletingListDeletesItsWordsButNotTheDictionary() {
        WordList list = service.create("Borrar");
        service.addItem(list.getId(), "σπίτι", "casa");
        service.addItem(list.getId(), "γάτα", "gato");
        long dictionaryBefore = dictionary.count();

        service.delete(list.getId());

        assertThat(service.findAll()).extracting(WordList::getName).doesNotContain("Borrar");
        assertThat(entityManager.createQuery("select count(i) from ListItem i", Long.class).getSingleResult())
                .isZero();
        assertThat(dictionary.count()).isEqualTo(dictionaryBefore);
    }

    @Test
    void filtersListsByNameIgnoringAccentsAndCase() {
        service.create("Verbos irregulares");
        service.create("Casa");
        service.create("Colores");

        assertThat(service.findFiltered("VERB", "name")).extracting(WordList::getName)
                .containsExactly("Verbos irregulares");
        assertThat(service.findFiltered("", "name")).extracting(WordList::getName)
                .containsExactly("Casa", "Colores", "Verbos irregulares");
    }

    @Test
    void sortsByNumberOfWordsWhenAsked() {
        service.create("Uno");
        WordList two = service.create("Dos");
        service.addCustomItem(two.getId(), "σπίτι", "casa");
        service.addCustomItem(two.getId(), "γάτα", "gato");

        assertThat(service.findFiltered("", "words")).extracting(WordList::getName)
                .containsExactly("Dos", "Uno");
    }

    @Test
    void editsWordInListWithoutChangingOtherLists() {
        WordList list = service.create("Editar");
        WordList other = service.create("Otra");
        ListItem item = service.addItem(list.getId(), "σπίτι", "casa");
        service.addCustomItem(other.getId(), "σπίτι", "casa"); // same pair in another list
        entityManager.flush();

        service.updateItem(list.getId(), item.getId(), "  οικία ", "hogar");

        ListItem stored = service.findWithItems(list.getId()).orElseThrow().getItems().get(0);
        assertThat(stored.getGreek()).isEqualTo("οικία");
        assertThat(stored.getSpanish()).isEqualTo("hogar");
        assertThat(service.findWithItems(other.getId()).orElseThrow().getItems().get(0).getSpanish()).isEqualTo("casa");
    }

    @Test
    void editRejectsBlankAndDuplicatePairs() {
        WordList list = service.create("Editar dos");
        ListItem first = service.addItem(list.getId(), "σπίτι", "casa");
        service.addItem(list.getId(), "γάτα", "gato");
        entityManager.flush();

        assertThatThrownBy(() -> service.updateItem(list.getId(), first.getId(), " ", "casa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both the Greek and the Spanish word are required.");
        assertThatThrownBy(() -> service.updateItem(list.getId(), first.getId(), "γάτα", "gato"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This word pair is already in the list.");
    }

    @Test
    void editRejectsWordNotInList() {
        WordList list = service.create("Sin palabra");

        assertThatThrownBy(() -> service.updateItem(list.getId(), 424_242L, "σπίτι", "casa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("That word is not in this list.");
    }

    @Test
    void renamesList() {
        WordList list = service.create("Viejo");

        WordList renamed = service.rename(list.getId(), "  Nuevo   nombre ");

        assertThat(renamed.getName()).isEqualTo("Nuevo nombre");
        assertThat(service.findAll()).extracting(WordList::getName).containsExactly("Nuevo nombre");
    }

    @Test
    void renameKeepsSameNameWithoutError() {
        WordList list = service.create("Igual");

        assertThat(service.rename(list.getId(), "Igual").getName()).isEqualTo("Igual");
    }

    @Test
    void renameRejectsBlankAndDuplicateNames() {
        WordList list = service.create("Uno");
        service.create("Dos");

        assertThatThrownBy(() -> service.rename(list.getId(), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("List name cannot be blank.");
        assertThatThrownBy(() -> service.rename(list.getId(), "Dos"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A list named \"Dos\" already exists.");
    }

}
