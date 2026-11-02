package com.gingy.repaso.gr.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes Greek and Spanish terms for comparison. No Spring dependencies.
 *
 * Steps: trim and collapse whitespace, lower-case, map final sigma to sigma,
 * remove accents and tonos, but keep the combining tilde so that ñ stays
 * different from n (año is not ano).
 */
public final class TermNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern COMBINING_MARK_EXCEPT_TILDE = Pattern.compile("[\\p{M}&&[^\\u0303]]");

    private TermNormalizer() {
    }

    public static String normalize(String term) {
        if (term == null) {
            return "";
        }
        String s = WHITESPACE.matcher(term.trim()).replaceAll(" ");
        s = s.toLowerCase(Locale.ROOT);
        s = s.replace('ς', 'σ');
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = COMBINING_MARK_EXCEPT_TILDE.matcher(s).replaceAll("");
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }
}
