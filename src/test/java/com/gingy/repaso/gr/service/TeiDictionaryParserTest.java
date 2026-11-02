package com.gingy.repaso.gr.service;

import com.gingy.repaso.gr.domain.DictionaryEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeiDictionaryParserTest {

    private static final String TEI = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE TEI SYSTEM "does-not-exist.dtd">
            <TEI xmlns="http://www.tei-c.org/ns/1.0">
              <teiHeader xml:lang="en">
                <fileDesc>
                  <editionStmt><edition>2023.05.29</edition></editionStmt>
                </fileDesc>
              </teiHeader>
              <text>
                <body xml:lang="el">
                  <entry>
                    <form><orth>σπίτι</orth><pron>ˈspi.ti</pron></form>
                    <gramGrp><pos>n</pos></gramGrp>
                    <sense>
                      <cit type="trans" xml:lang="es"><quote>casa, hogar</quote></cit>
                      <sense>
                        <cit type="trans" xml:lang="es"><quote>casa / hogar</quote></cit>
                      </sense>
                    </sense>
                  </entry>
                  <entry>
                    <form><orth>γάτα</orth></form>
                    <sense>
                      <cit type="trans" xml:lang="en"><quote>cat</quote></cit>
                      <cit type="trans" xml:lang="es"><quote>gato</quote></cit>
                    </sense>
                  </entry>
                  <entry>
                    <form><orth>   </orth></form>
                    <sense><cit type="trans" xml:lang="es"><quote>nada</quote></cit></sense>
                  </entry>
                </body>
              </text>
            </TEI>
            """;

    @TempDir
    Path dir;

    @Test
    void splitsTranslationsDeduplicatesAndSkipsOtherLanguagesAndBlankHeadwords() throws Exception {
        Path file = dir.resolve("test.tei");
        Files.writeString(file, TEI);

        TeiDictionaryParser.ParsedDictionary parsed = new TeiDictionaryParser().parse(file);

        assertThat(parsed.source()).isEqualTo("FreeDict+WikDict ell-spa 2023.05.29");
        List<String> pairs = parsed.entries().stream()
                .map(e -> e.getGreek() + "=" + e.getSpanish())
                .toList();
        assertThat(pairs).containsExactly("σπίτι=casa", "σπίτι=hogar", "γάτα=gato");
    }

    @Test
    void storesNormalizedKeys() throws Exception {
        Path file = dir.resolve("test.tei");
        Files.writeString(file, TEI);

        DictionaryEntry first = new TeiDictionaryParser().parse(file).entries().get(0);

        assertThat(first.getGreekKey()).isEqualTo("σπιτι");
        assertThat(first.getSpanishKey()).isEqualTo("casa");
    }
}
