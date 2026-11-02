package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.domain.WordList;
import com.gingy.repaso.gr.service.WordListService;
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

import java.util.List;

@Controller
@RequestMapping("/lists")
public class ListController {

    private final WordListService wordListService;

    public ListController(WordListService wordListService) {
        this.wordListService = wordListService;
    }

    @GetMapping
    public String showLists(Model model) {
        model.addAttribute("lists", wordListService.findAll());
        return "lists";
    }

    /** Post/Redirect/Get: on success or error, redirect back to the page. */
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
        model.addAttribute("matches", query.isEmpty() ? List.of() : wordListService.findDictionaryMatches(query));
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
