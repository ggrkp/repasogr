package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.domain.WordList;
import com.gingy.repaso.gr.service.WordListService;
import com.gingy.repaso.gr.service.WordListService.WordMatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ListController.class)
class ListControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WordListService service;

    @Test
    void showsExistingLists() throws Exception {
        given(service.findFiltered("", "name")).willReturn(List.of(new WordList("Verbos")));

        mvc.perform(get("/lists"))
                .andExpect(status().isOk())
                .andExpect(view().name("lists"))
                .andExpect(model().attributeExists("lists"))
                .andExpect(content().string(containsString("Verbos")));
    }

    @Test
    void createsListAndRedirects() throws Exception {
        mvc.perform(post("/lists").param("name", "Verbos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists"));

        verify(service).create("Verbos");
    }

    @Test
    void showsErrorWhenCreationFails() throws Exception {
        doThrow(new IllegalArgumentException("List name cannot be blank."))
                .when(service).create(anyString());

        mvc.perform(post("/lists").param("name", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists"))
                .andExpect(flash().attribute("error", "List name cannot be blank."));
    }

    @Test
    void showsListPage() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));

        mvc.perform(get("/lists/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("list"))
                .andExpect(content().string(containsString("Verbos")));
    }

    @Test
    void unknownListReturnsNotFound() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.empty());

        mvc.perform(get("/lists/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addsItemAndRedirectsToList() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));

        mvc.perform(post("/lists/1/items").param("greek", "σπίτι").param("spanish", "casa"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/1"));

        verify(service).addItem(1L, "σπίτι", "casa");
    }

    @Test
    void addsOwnItemAndRedirectsToList() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));

        mvc.perform(post("/lists/1/items/custom").param("greek", "ψωμί").param("spanish", "pan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/1"))
                .andExpect(flash().attribute("message", "Word added."));

        verify(service).addCustomItem(1L, "ψωμί", "pan");
    }

    @Test
    void noDictionaryMatchShowsOwnWordFormPrefilledWithSearch() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));
        given(service.findDictionaryMatches(anyString())).willReturn(List.of());

        mvc.perform(get("/lists/1").param("word", "ψωμί"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/items/custom")))
                .andExpect(content().string(containsString("value=\"ψωμί\"")));
    }

    @Test
    void editsListWordAndRedirectsToList() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));

        mvc.perform(post("/lists/1/items/5/edit").param("greek", "σπίτι").param("spanish", "hogar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/1"))
                .andExpect(flash().attribute("message", "Word updated."));

        verify(service).updateItem(1L, 5L, "σπίτι", "hogar");
    }

    @Test
    void listsPageFiltersAndSortsByQueryAndSort() throws Exception {
        given(service.findFiltered("ver", "words")).willReturn(List.of(new WordList("Verbos")));

        mvc.perform(get("/lists").param("q", "ver").param("sort", "words"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Verbos")))
                .andExpect(content().string(containsString("value=\"ver\"")));
    }

    @Test
    void rejectedItemShowsErrorAndKeepsInput() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));
        doThrow(new IllegalArgumentException("\"σπίτι\" = \"perro\" is not in the dictionary."))
                .when(service).addItem(any(), anyString(), anyString());

        mvc.perform(post("/lists/1/items").param("greek", "σπίτι").param("spanish", "perro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/1"))
                .andExpect(flash().attribute("error", "\"σπίτι\" = \"perro\" is not in the dictionary."));
    }

    @Test
    void searchShowsDictionaryMatchesWithAddButtons() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));
        given(service.findDictionaryMatches("σπίτι")).willReturn(List.of(
                new WordMatch("σπίτι", "casa", false),
                new WordMatch("σπίτι", "hogar", true)));

        mvc.perform(get("/lists/1").param("word", "σπίτι"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("casa")))
                .andExpect(content().string(containsString("hogar")))
                .andExpect(content().string(containsString("name=\"spanish\" value=\"casa\"")))
                .andExpect(content().string(containsString("My words")));
    }

    @Test
    void searchWithNoMatchShowsMessage() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));
        given(service.findDictionaryMatches("xyz")).willReturn(List.of());

        mvc.perform(get("/lists/1").param("word", "xyz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No dictionary entry for")));
    }

    @Test
    void addItemToUnknownListReturnsNotFound() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.empty());

        mvc.perform(post("/lists/99/items").param("greek", "σπίτι").param("spanish", "casa"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removesItemAndRedirectsToList() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));

        mvc.perform(post("/lists/1/items/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/1"));

        verify(service).removeItem(1L, 5L);
    }

    @Test
    void deletesListAndRedirectsToOverview() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));

        mvc.perform(post("/lists/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists"))
                .andExpect(flash().attribute("message", "List deleted."));

        verify(service).delete(1L);
    }

    @Test
    void deletingUnknownListReturnsNotFound() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.empty());

        mvc.perform(post("/lists/99/delete"))
                .andExpect(status().isNotFound());
    }

    @Test
    void renamesListAndRedirectsToList() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));

        mvc.perform(post("/lists/1/rename").param("name", "Nuevo"))
                .andExpect(redirectedUrl("/lists/1"))
                .andExpect(flash().attribute("message", "List renamed."));

        verify(service).rename(1L, "Nuevo");
    }

    @Test
    void renameErrorIsShownOnList() throws Exception {
        given(service.findWithItems(anyLong())).willReturn(Optional.of(new WordList("Verbos")));
        doThrow(new IllegalArgumentException("List name cannot be blank."))
                .when(service).rename(anyLong(), anyString());

        mvc.perform(post("/lists/1/rename").param("name", " "))
                .andExpect(redirectedUrl("/lists/1"))
                .andExpect(flash().attribute("error", "List name cannot be blank."));
    }

    @Test
    void combinesListsAndRedirectsToNewList() throws Exception {
        WordList created = mock(WordList.class);
        given(created.getId()).willReturn(5L);
        given(service.createCombined("Juntas", List.of(1L, 2L))).willReturn(created);

        mvc.perform(post("/lists/combine").param("name", "Juntas").param("sourceIds", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists/5"));

        verify(service).createCombined("Juntas", List.of(1L, 2L));
    }

    @Test
    void showsErrorWhenCombiningFails() throws Exception {
        doThrow(new IllegalArgumentException("Choose at least one list to combine."))
                .when(service).createCombined(anyString(), any());

        mvc.perform(post("/lists/combine").param("name", "Juntas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lists"))
                .andExpect(flash().attribute("error", "Choose at least one list to combine."));
    }
}

