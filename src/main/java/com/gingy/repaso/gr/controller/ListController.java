package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.domain.WordList;
import com.gingy.repaso.gr.service.WordListService;
import com.gingy.repaso.gr.service.WordListService.WordMatch;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/lists")
public class ListController {

    private static final Pattern GREEK_LETTER = Pattern.compile("\\p{IsGreek}");

    private final WordListService wordListService;

    public ListController(WordListService wordListService) {
        this.wordListService = wordListService;
    }

    @GetMapping
    public String showLists(@RequestParam(name = "q", required = false) String query,
                            @RequestParam(name = "sort", defaultValue = "name") String sort,
                            Model model) {
        String q = query == null ? "" : query.trim();
        model.addAttribute("lists", wordListService.findFiltered(q, sort));
        model.addAttribute("allLists", wordListService.findAll()); // the combine form always offers every list
        model.addAttribute("q", q);
        model.addAttribute("sort", "words".equals(sort) ? "words" : "name");
        return "lists";
    }

    /** Post/Redirect/Get: on success or error, redirect back to the page. */
    /** Creates a list holding the words of the chosen lists, then opens the new list. */
    @PostMapping("/combine")
    public String combineLists(@RequestParam(name = "name", defaultValue = "") String name,
                               @RequestParam(name = "sourceIds", required = false) List<Long> sourceIds,
                               RedirectAttributes redirect) {
        try {
            WordList combined = wordListService.createCombined(name, sourceIds);
            int count = new HashSet<>(sourceIds).size();
            redirect.addFlashAttribute("message", "List created from " + count
                    + (count == 1 ? " list." : " lists."));
            return "redirect:/lists/" + combined.getId();
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("name", name);
            return "redirect:/lists";
        }
    }

    @PostMapping
    public String createList(@RequestParam(name = "name", defaultValue = "") String name,
                             RedirectAttributes redirect) {
        try {
            wordListService.create(name);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("name", name);
        }
        return "redirect:/lists";
    }

    /** Shows the list. With ?word=..., also shows the dictionary pairs that match the word. */
    @GetMapping("/{id}")
    public String showList(@PathVariable Long id,
                           @RequestParam(name = "word", required = false) String word,
                           Model model) {
        WordList list = findListOr404(id);
        String query = word == null ? "" : word.trim();
        model.addAttribute("list", list);
        model.addAttribute("word", query);
        List<WordMatch> matches = query.isEmpty() ? List.of() : wordListService.findDictionaryMatches(query);
        model.addAttribute("matches", matches);
        // Pre-fill the manual form: the searched word goes in the Greek or Spanish box by its script.
        boolean greekQuery = GREEK_LETTER.matcher(query).find();
        model.addAttribute("manualGreek", greekQuery ? query : "");
        model.addAttribute("manualSpanish", greekQuery || query.isEmpty() ? "" : query);
        return "list";
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable Long id,
                          @RequestParam(name = "greek", defaultValue = "") String greek,
                          @RequestParam(name = "spanish", defaultValue = "") String spanish,
                          RedirectAttributes redirect) {
        findListOr404(id);
        try {
            wordListService.addItem(id, greek, spanish);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lists/" + id;
    }

    /** Adds a pair the user typed, used when the dictionary has no entry for the word. */
    @PostMapping("/{id}/items/custom")
    public String addCustomItem(@PathVariable Long id,
                                @RequestParam(name = "greek", defaultValue = "") String greek,
                                @RequestParam(name = "spanish", defaultValue = "") String spanish,
                                RedirectAttributes redirect) {
        findListOr404(id);
        try {
            wordListService.addCustomItem(id, greek, spanish);
            redirect.addFlashAttribute("message", "Word added.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lists/" + id;
    }

    @PostMapping("/{id}/items/{itemId}/edit")
    public String editItem(@PathVariable Long id,
                           @PathVariable Long itemId,
                           @RequestParam(name = "greek", defaultValue = "") String greek,
                           @RequestParam(name = "spanish", defaultValue = "") String spanish,
                           RedirectAttributes redirect) {
        findListOr404(id);
        try {
            wordListService.updateItem(id, itemId, greek, spanish);
            redirect.addFlashAttribute("message", "Word updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lists/" + id;
    }

    @PostMapping("/{id}/items/{itemId}/delete")
    public String removeItem(@PathVariable Long id, @PathVariable Long itemId, RedirectAttributes redirect) {
        findListOr404(id);
        try {
            wordListService.removeItem(id, itemId);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lists/" + id;
    }

    @PostMapping("/{id}/rename")
    public String rename(@PathVariable Long id,
                         @RequestParam(name = "name", defaultValue = "") String name,
                         RedirectAttributes redirect) {
        findListOr404(id);
        try {
            wordListService.rename(id, name);
            redirect.addFlashAttribute("message", "List renamed.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lists/" + id;
    }

    /** Deletes the list and its words, then returns to the list overview. */
    @PostMapping("/{id}/delete")
    public String deleteList(@PathVariable Long id, RedirectAttributes redirect) {
        findListOr404(id);
        wordListService.delete(id);
        redirect.addFlashAttribute("message", "List deleted.");
        return "redirect:/lists";
    }

    private WordList findListOr404(Long id) {
        return wordListService.findWithItems(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No list with id " + id));
    }
}
