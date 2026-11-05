package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.domain.OwnWord;
import com.gingy.repaso.gr.service.OwnWordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** My words: the user's own saved pairs. View, search, add, edit and delete. */
@Controller
@RequestMapping("/words")
public class WordController {

    private final OwnWordService ownWordService;

    public WordController(OwnWordService ownWordService) {
        this.ownWordService = ownWordService;
    }

    @GetMapping
    public String showWords(@RequestParam(name = "q", required = false) String query, Model model) {
        String q = query == null ? "" : query.trim();
        List<OwnWord> words = ownWordService.search(q);
        model.addAttribute("q", q);
        model.addAttribute("words", words);
        return "words";
    }

    @PostMapping
    public String createWord(@RequestParam(name = "greek", defaultValue = "") String greek,
                             @RequestParam(name = "spanish", defaultValue = "") String spanish,
                             RedirectAttributes redirect) {
        try {
            ownWordService.saveIfAbsent(greek, spanish);
            redirect.addFlashAttribute("message", "Word saved.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/words";
    }

    @PostMapping("/{id}")
    public String updateWord(@PathVariable Long id,
                             @RequestParam(name = "greek", defaultValue = "") String greek,
                             @RequestParam(name = "spanish", defaultValue = "") String spanish,
                             @RequestParam(name = "q", defaultValue = "") String query,
                             RedirectAttributes redirect) {
        try {
            ownWordService.update(id, greek, spanish);
            redirect.addFlashAttribute("message", "Word updated.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return redirectTo(query);
    }

    @PostMapping("/{id}/delete")
    public String deleteWord(@PathVariable Long id,
                             @RequestParam(name = "q", defaultValue = "") String query,
                             RedirectAttributes redirect) {
        try {
            ownWordService.delete(id);
            redirect.addFlashAttribute("message", "Word deleted.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return redirectTo(query);
    }

    private static String redirectTo(String query) {
        return query == null || query.isBlank() ? "redirect:/words" : "redirect:/words?q=" + encode(query);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
