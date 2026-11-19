package com.monomaxia.gnoseon;

import android.net.Uri;

/**
 * Players suggest questions through a Google Form. The app opens the form in the browser with the
 * player's text already filled in; the player submits it there. The app itself sends nothing and
 * needs no internet permission.
 *
 * To connect a form: in Google Forms choose "Get pre-filled link", fill every field with any text,
 * copy the link, and put its address and the "entry.NNN" numbers below, in the same order as the
 * fields. Leave FORM_URL empty to show the screen without sending.
 */
final class SuggestionForm {
    /** The form's address, ending in /viewform. */
    static final String FORM_URL =
            "https://docs.google.com/forms/d/e/1FAIpQLSc8gFyiehlaxf2BBd36PLoV0QQPCAdsCs3ScgxYHeMNOklV9w/viewform";
    static final String ENTRY_CATEGORY = "entry.984991535";
    static final String ENTRY_DIFFICULTY = "entry.161876267";
    static final String ENTRY_QUESTION = "entry.423914536";
    static final String ENTRY_ANSWER = "entry.1869822672";
    static final String ENTRY_SOURCE = "entry.506232911";
    static final String ENTRY_NAME = "entry.642158415";

    /** Must match the form's choices exactly, or the field opens empty. */
    static final String[] DIFFICULTIES = {"Εύκολη", "Μεσαία", "Δύσκολη"};

    static final int MAX_QUESTION = 300;
    static final int MAX_ANSWER = 120;
    static final int MAX_SOURCE = 200;
    static final int MAX_NAME = 40;

    private SuggestionForm() { }

    static boolean isConfigured() {
        return !FORM_URL.isEmpty() && !ENTRY_QUESTION.isEmpty() && !ENTRY_ANSWER.isEmpty();
    }

    /** The form's address with the player's answers filled in, or null if no form is set up. */
    static Uri prefilledUri(String category, String difficulty, String question, String answer,
                            String source, String name) {
        if (!isConfigured()) return null;
        Uri.Builder b = Uri.parse(FORM_URL).buildUpon().appendQueryParameter("usp", "pp_url");
        add(b, ENTRY_CATEGORY, category);
        add(b, ENTRY_DIFFICULTY, difficulty);
        add(b, ENTRY_QUESTION, question);
        add(b, ENTRY_ANSWER, answer);
        add(b, ENTRY_SOURCE, source);
        add(b, ENTRY_NAME, name);
        return b.build();
    }

    private static void add(Uri.Builder b, String entry, String value) {
        if (!entry.isEmpty() && value != null && !value.trim().isEmpty()) {
            b.appendQueryParameter(entry, value.trim());
        }
    }
}
