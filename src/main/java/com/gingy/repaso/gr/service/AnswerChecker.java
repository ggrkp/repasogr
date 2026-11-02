package com.gingy.repaso.gr.service;

import java.util.regex.Pattern;

/**
 * Checks a typed answer against a stored term. No Spring dependencies.
 *
 * A stored term may hold several accepted answers separated by ",", ";" or "/".
 * The answer is correct if it matches any of them after normalization.
 */
public final class AnswerChecker {

    private static final Pattern SEPARATORS = Pattern.compile("[,;/]");

    private AnswerChecker() {
    }

    public static String normalize(String term) {
        return TermNormalizer.normalize(term);
    }

    public static boolean isCorrect(String userAnswer, String storedTerm) {
        String answer = normalize(userAnswer);
        if (answer.isEmpty() || storedTerm == null) {
            return false;
        }
        for (String part : SEPARATORS.split(storedTerm)) {
            if (normalize(part).equals(answer)) {
                return true;
            }
        }
        return false;
    }
}
