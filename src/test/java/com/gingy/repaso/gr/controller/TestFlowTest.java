package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.domain.WordList;
import com.gingy.repaso.gr.service.DictionaryService;
import com.gingy.repaso.gr.service.WordListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Whole flow through the web layer: create a list, add words, take tests, retake missed words. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testflow;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class TestFlowTest {

    private static final String TEI = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TEI xmlns="http://www.tei-c.org/ns/1.0">
              <teiHeader><fileDesc><editionStmt><edition>test</edition></editionStmt></fileDesc></teiHeader>
              <text><body xml:lang="el">
                <entry>
                  <form><orth>σπίτι</orth></form>
                  <sense><cit type="trans" xml:lang="es"><quote>casa</quote></cit></sense>
                </entry>
                <entry>
                  <form><orth>γάτα</orth></form>
                  <sense><cit type="trans" xml:lang="es"><quote>gato</quote></cit></sense>
                </entry>
              </body></text>
            </TEI>
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    DictionaryService dictionary;

    @Autowired
    WordListService lists;

    @TempDir
    Path dir;

    private long listId;

    @BeforeEach
    void setUp() throws Exception {
        Path file = dir.resolve("ell-spa.tei");
        Files.writeString(file, TEI);
        dictionary.importTei(file);

        mvc.perform(post("/lists").param("name", "Básico")).andExpect(status().is3xxRedirection());
        WordList list = lists.findAll().stream().filter(l -> l.getName().equals("Básico")).findFirst().orElseThrow();
        listId = list.getId();
        mvc.perform(post("/lists/{id}/items", listId).param("greek", "σπίτι").param("spanish", "casa"))
                .andExpect(redirectedUrl("/lists/" + listId));
        mvc.perform(post("/lists/{id}/items", listId).param("greek", "γάτα").param("spanish", "gato"))
                .andExpect(redirectedUrl("/lists/" + listId));
    }

    @Test
    void rejectsWordPairNotInDictionary() throws Exception {
        mvc.perform(post("/lists/{id}/items", listId).param("greek", "σπίτι").param("spanish", "perro"))
                .andExpect(redirectedUrl("/lists/" + listId));
        mvc.perform(get("/lists/{id}", listId))
                .andExpect(content().string(not(containsString("perro"))));
    }

    @Test
    void perfectTestShowsFullScore() throws Exception {
        MockHttpSession session = startTest("EL_TO_ES");

        answerBothCorrectly(session, "EL_TO_ES");

        mvc.perform(get("/lists/{id}/test/result", listId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Score: 2 / 2")))
                .andExpect(content().string(containsString("No words missed.")));
    }

    @Test
    void spanishToGreekTestAcceptsAccentlessAnswers() throws Exception {
        MockHttpSession session = startTest("ES_TO_EL");

        answerBothCorrectly(session, "ES_TO_EL");

        mvc.perform(get("/lists/{id}/test/result", listId).session(session))
                .andExpect(content().string(containsString("Score: 2 / 2")));
    }

    @Test
    void missedWordsCanBeRetakenAlone() throws Exception {
        MockHttpSession session = startTest("EL_TO_ES");

        // Answer the first question wrongly, the second correctly.
        String firstPrompt = promptOf(mvc.perform(get("/lists/{id}/test/question", listId).session(session))
                .andReturn().getResponse().getContentAsString());
        boolean firstIsCat = firstPrompt.equals("γάτα");
        mvc.perform(post("/lists/{id}/test/answer", listId).param("answer", "wrong").session(session));
        String secondPrompt = promptOf(mvc.perform(get("/lists/{id}/test/question", listId).session(session))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post("/lists/{id}/test/answer", listId)
                .param("answer", correctAnswerFor(secondPrompt, "EL_TO_ES")).session(session));

        String missedWord = firstIsCat ? "γάτα" : "σπίτι";
        mvc.perform(get("/lists/{id}/test/result", listId).session(session))
                .andExpect(content().string(containsString("Score: 1 / 2")))
                .andExpect(content().string(containsString("Retake missed words only")))
                .andExpect(content().string(containsString(missedWord)));

        mvc.perform(get("/lists/{id}/test/retake-missed", listId).session(session))
                .andExpect(redirectedUrl("/lists/" + listId + "/test/question"));
        mvc.perform(get("/lists/{id}/test/question", listId).session(session))
                .andExpect(content().string(containsString(missedWord)))
                .andExpect(content().string(containsString("1 of 1")));
    }

    @Test
    void testWithoutSessionRedirectsToList() throws Exception {
        mvc.perform(get("/lists/{id}/test/question", listId))
                .andExpect(redirectedUrl("/lists/" + listId));
    }

    @Test
    void testOfEmptyListIsRefused() throws Exception {
        mvc.perform(post("/lists").param("name", "Vacía"));
        long emptyId = lists.findAll().stream().filter(l -> l.getName().equals("Vacía")).findFirst()
                .orElseThrow().getId();

        mvc.perform(get("/lists/{id}/test", emptyId).param("direction", "EL_TO_ES"))
                .andExpect(redirectedUrl("/lists/" + emptyId));
    }

    @Test
    void unknownListReturnsNotFound() throws Exception {
        mvc.perform(get("/lists/999999/test").param("direction", "EL_TO_ES"))
                .andExpect(status().isNotFound());
    }

    private MockHttpSession startTest(String direction) throws Exception {
        MvcResult result = mvc.perform(get("/lists/{id}/test", listId).param("direction", direction))
                .andExpect(redirectedUrl("/lists/" + listId + "/test/question"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }

    /** Answers both questions with the correct translation, whatever the order. */
    private void answerBothCorrectly(MockHttpSession session, String direction) throws Exception {
        for (int i = 0; i < 2; i++) {
            String prompt = promptOf(mvc.perform(get("/lists/{id}/test/question", listId).session(session))
                    .andReturn().getResponse().getContentAsString());
            mvc.perform(post("/lists/{id}/test/answer", listId)
                    .param("answer", correctAnswerFor(prompt, direction)).session(session));
        }
    }

    /** The word shown as the question. Read from the prompt element, not the feedback text. */
    private static String promptOf(String page) {
        Matcher m = Pattern.compile("class=\"prompt\">([^<]+)<").matcher(page);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static String correctAnswerFor(String prompt, String direction) {
        boolean greekShown = "EL_TO_ES".equals(direction);
        return switch (prompt) {
            case "σπίτι" -> greekShown ? "casa" : "σπίτι";
            case "γάτα" -> greekShown ? "gato" : "γάτα";
            case "casa" -> "σπίτι";
            case "gato" -> "γάτα";
            default -> throw new IllegalStateException("Unexpected prompt: " + prompt);
        };
    }

    @Test
    void removedWordDisappearsFromListAndDeletedListIsGone() throws Exception {
        WordList list = lists.findWithItems(listId).orElseThrow();
        Long catId = list.getItems().stream().filter(i -> i.getGreek().equals("γάτα")).findFirst()
                .orElseThrow().getId();

        mvc.perform(post("/lists/{id}/items/{itemId}/delete", listId, catId))
                .andExpect(redirectedUrl("/lists/" + listId));
        mvc.perform(get("/lists/{id}", listId))
                .andExpect(content().string(not(containsString("γάτα"))))
                .andExpect(content().string(containsString("σπίτι")));

        mvc.perform(post("/lists/{id}/delete", listId))
                .andExpect(redirectedUrl("/lists"));
        mvc.perform(get("/lists/{id}", listId))
                .andExpect(status().isNotFound());
    }
}
