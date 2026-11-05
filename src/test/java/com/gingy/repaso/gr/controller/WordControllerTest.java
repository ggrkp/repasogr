package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.service.OwnWordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WordController.class)
class WordControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OwnWordService service;

    @Test
    void showsAllWordsWithoutQuery() throws Exception {
        given(service.search("")).willReturn(List.of());

        mvc.perform(get("/words"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No words yet.")));
    }

    @Test
    void searchPassesQueryToService() throws Exception {
        given(service.search("pan")).willReturn(List.of());

        mvc.perform(get("/words").param("q", "pan"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No words match.")));

        verify(service).search("pan");
    }

    @Test
    void createSavesAndRedirects() throws Exception {
        mvc.perform(post("/words").param("greek", "ψωμί").param("spanish", "pan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/words"))
                .andExpect(flash().attribute("message", "Word saved."));

        verify(service).saveIfAbsent("ψωμί", "pan");
    }

    @Test
    void updateReturnsToSearchWithError() throws Exception {
        doThrow(new IllegalArgumentException("This word pair is already in your words."))
                .when(service).update(anyLong(), anyString(), anyString());

        mvc.perform(post("/words/3").param("greek", "ψωμί").param("spanish", "pan").param("q", "pan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/words?q=pan"))
                .andExpect(flash().attribute("error", "This word pair is already in your words."));
    }

    @Test
    void deleteRedirectsBackToList() throws Exception {
        mvc.perform(post("/words/3/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/words"))
                .andExpect(flash().attribute("message", "Word deleted."));

        verify(service).delete(3L);
    }
}
