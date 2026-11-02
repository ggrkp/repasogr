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
@Import({WordListService.class, DictionaryService.class, TeiDictionaryParser.class})
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
