package com.gingy.repaso.gr.controller;

import com.gingy.repaso.gr.domain.Direction;
import com.gingy.repaso.gr.domain.ListItem;
import com.gingy.repaso.gr.domain.WordList;
import com.gingy.repaso.gr.service.TestService;
import com.gingy.repaso.gr.service.WordListService;
import jakarta.servlet.http.HttpSession;
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

@Controller
@RequestMapping("/lists/{listId}/test")
public class TestController {

    static final String SESSION_KEY = "testSession";

    private final WordListService wordListService;
    private final TestService testService;

    public TestController(WordListService wordListService, TestService testService) {
        this.wordListService = wordListService;
        this.testService = testService;
    }

    @GetMapping
    public String start(@PathVariable Long listId,
                        @RequestParam Direction direction,
                        HttpSession session,
                        RedirectAttributes redirect) {
        WordList list = findListOr404(listId);
        if (list.getItems().isEmpty()) {
            redirect.addFlashAttribute("error", "This list has no words to test yet.");
            return "redirect:/lists/" + listId;
        }
        session.setAttribute(SESSION_KEY, testService.start(list, direction));
        return "redirect:/lists/" + listId + "/test/question";
    }

    @GetMapping("/question")
    public String question(@PathVariable Long listId, HttpSession session, Model model) {
        WordList list = findListOr404(listId);
        TestSession test = activeSession(session, listId);
        if (test == null) {
            return "redirect:/lists/" + listId;
        }
        if (test.isFinished()) {
            return "redirect:/lists/" + listId + "/test/result";
        }
        ListItem item = testService.findItem(list, test.currentItemId()).orElse(null);
        if (item == null) {
            session.removeAttribute(SESSION_KEY);
            return "redirect:/lists/" + listId;
        }
        model.addAttribute("listId", listId);
        model.addAttribute("listName", list.getName());
        model.addAttribute("prompt", testService.prompt(item, test.getDirection()));
        model.addAttribute("position", test.position());
        model.addAttribute("total", test.total());
        model.addAttribute("hint", test.isHintShown() ? testService.hint(item, test.getDirection()) : null);
        return "question";
    }

    /** Shows the first letter of the expected answer for the current question. */
    @PostMapping("/hint")
    public String hint(@PathVariable Long listId, HttpSession session) {
        TestSession test = activeSession(session, listId);
        if (test != null && !test.isFinished()) {
            test.showHint();
        }
        return "redirect:/lists/" + listId + "/test/question";
    }

    /** Checks the answer, then shows feedback on the next page (flash attributes). */
    @PostMapping("/answer")
    public String answer(@PathVariable Long listId,
                         @RequestParam(name = "answer", defaultValue = "") String answer,
                         HttpSession session,
                         RedirectAttributes redirect) {
        WordList list = findListOr404(listId);
        TestSession test = activeSession(session, listId);
        if (test == null) {
            return "redirect:/lists/" + listId;
        }
        if (test.isFinished()) {
            return "redirect:/lists/" + listId + "/test/result";
        }
        ListItem item = testService.findItem(list, test.currentItemId()).orElse(null);
        if (item == null) {
            session.removeAttribute(SESSION_KEY);
            return "redirect:/lists/" + listId;
        }

        boolean correct = testService.check(item, test.getDirection(), answer);
        test.record(item.getId(), correct);

        redirect.addFlashAttribute("answerCorrect", correct);
        redirect.addFlashAttribute("answerGiven", answer);
        redirect.addFlashAttribute("answerPrompt", testService.prompt(item, test.getDirection()));
        redirect.addFlashAttribute("answerExpected", testService.expected(item, test.getDirection()));
        return test.isFinished()
                ? "redirect:/lists/" + listId + "/test/result"
                : "redirect:/lists/" + listId + "/test/question";
    }

    @GetMapping("/result")
    public String result(@PathVariable Long listId, HttpSession session, Model model) {
        WordList list = findListOr404(listId);
        TestSession test = activeSession(session, listId);
        if (test == null) {
            return "redirect:/lists/" + listId;
        }
        if (!test.isFinished()) {
            return "redirect:/lists/" + listId + "/test/question";
        }
        model.addAttribute("listId", listId);
        model.addAttribute("listName", list.getName());
        model.addAttribute("correct", test.correctCount());
        model.addAttribute("total", test.total());
        model.addAttribute("missed", testService.missedWords(list, test));
        return "result";
    }

    /** Starts a new test over only the words missed in the finished test. */
    @GetMapping("/retake-missed")
    public String retakeMissed(@PathVariable Long listId, HttpSession session) {
        WordList list = findListOr404(listId);
        TestSession test = activeSession(session, listId);
        if (test == null) {
            return "redirect:/lists/" + listId;
        }
        if (!test.isFinished()) {
            return "redirect:/lists/" + listId + "/test/question";
        }
        if (test.getWrongItemIds().isEmpty()) {
            return "redirect:/lists/" + listId + "/test/result";
        }
        session.setAttribute(SESSION_KEY, testService.retakeMissed(test));
        return "redirect:/lists/" + listId + "/test/question";
    }

    /** The test in the session, if it belongs to this list. */
    private static TestSession activeSession(HttpSession session, Long listId) {
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof TestSession test && listId.equals(test.getListId())) {
            return test;
        }
        return null;
    }

    private WordList findListOr404(Long id) {
        return wordListService.findWithItems(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No list with id " + id));
    }
}
