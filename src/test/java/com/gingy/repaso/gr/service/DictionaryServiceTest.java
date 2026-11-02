package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.DictionaryEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({DictionaryService.class, TeiDictionaryParser.class})
class DictionaryServiceTest {

    private static final String TEI = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TEI xmlns="http://www.tei-c.org/ns/1.0">
              <teiHeader><fileDesc><editionStmt><edition>2023.05.29</edition></editionStmt></fileDesc></teiHeader>
              <text><body xml:lang="el">
                <entry>
                  <form><orth>σπίτι</orth></form>
                  <sense><cit type="trans" xml:lang="es"><quote>casa, hogar</quote></cit></sense>
                </entry>
                <entry>
                  <form><orth>año</orth></form>
                  <sense><cit type="trans" xml:lang="es"><quote>año</quote></cit></sense>
                </entry>
              </body></text>
            </TEI>
            """;

    @Autowired
    DictionaryService service;

    @TempDir
    Path dir;

    @Test
    void looksUpBothDirectionsIgnoringAccentsAndCase() throws Exception {
        Path file = dir.resolve("ell-spa.tei");
        Files.writeString(file, TEI);
        assertThat(service.importTei(file)).isEqualTo(3);

        assertThat(spanishOf(service.findByGreek("ΣΠΙΤΙ"))).containsExactly("casa", "hogar");
        assertThat(greekOf(service.findBySpanish("Hogar"))).containsExactly("σπίτι");
        assertThat(greekOf(service.findBySpanish("año"))).containsExactly("año");
        assertThat(service.findBySpanish("ano")).isEmpty();
    }

    @Test
    void importingTheSameFileAgainDoesNotDuplicate() throws Exception {
        Path file = dir.resolve("ell-spa.tei");
        Files.writeString(file, TEI);
        service.importTei(file);
        service.importTei(file);

        assertThat(service.count()).isEqualTo(3);
    }

    @Test
    void findMatchesCombinesBothDirectionsWithoutDuplicates() throws Exception {
        Path file = dir.resolve("ell-spa.tei");
        Files.writeString(file, TEI);
        service.importTei(file);

        assertThat(service.findMatches("σπίτι"))
                .extracting(DictionaryEntry::getSpanish).containsExactly("casa", "hogar");
        assertThat(service.findMatches("casa"))
                .extracting(DictionaryEntry::getGreek).containsExactly("σπίτι");
        assertThat(service.findMatches("")).isEmpty();
    }

    @Test
    void blankTermReturnsNothing() {
        assertThat(service.findByGreek("   ")).isEmpty();
        assertThat(service.findBySpanish("")).isEmpty();
    }

    private static List<String> spanishOf(List<DictionaryEntry> entries) {
        return entries.stream().map(DictionaryEntry::getSpanish).toList();
    }

    private static List<String> greekOf(List<DictionaryEntry> entries) {
        return entries.stream().map(DictionaryEntry::getGreek).toList();
    }
}
