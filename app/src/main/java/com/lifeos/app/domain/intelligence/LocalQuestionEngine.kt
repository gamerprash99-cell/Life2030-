package com.lifeos.app.domain.intelligence

import java.time.LocalDate

/**
 * Picks one reflective question to put in front of the user.
 *
 * There is no model and no network here, and that is the whole design: the
 * questions come from a fixed table written by hand, chosen deterministically
 * from the date. The same day always produces the same question, two devices
 * with the same journal produce the same question, and the questions cannot be
 * wrong because nothing is being inferred — nothing about the user is
 * analysed to *produce* the text, only to decide which of a known set applies.
 *
 * The questions are also deliberately open and concrete. Nothing here asks the
 * user to diagnose themselves, and nothing references a mood as a finding
 * ("why were you anxious this week?") — that would be reading a state into a
 * handful of taps and dressing it up as insight.
 */
object LocalQuestionEngine {

    private val PRESENT = listOf(
        "What is one thing you would like to remember about today?",
        "What did you notice today that you would normally walk past?",
        "Where did your attention go most of today?",
        "What felt easier today than it usually does?",
        "What is something you are quietly proud of from today?",
        "If today were a page in a book, what would its heading be?",
        "What did you do today that your future self will thank you for?",
        "What conversation are you still turning over?",
        "What is one small thing that made today better?",
        "What would you have done differently today, and why not?",
        "What are you carrying into tomorrow?",
        "What did today make you curious about?",
        "When did you last feel fully absorbed in something?",
        "What did you make, fix, or finish today?",
        "What is going on in your head that you have not said out loud?"
    )

    private val PAST = listOf(
        "Look back at an entry you wrote this week. What has changed since?",
        "Find something you wrote a month ago. What would you tell that version of yourself?",
        "Read an older entry. What did you not know then?",
        "Which entry from this year would you want to read again in ten years?",
        "Go back to a memory you were not sure about. What do you think now?",
        "Find a line you wrote before. Does it still sound like you?"
    )

    /**
     * Theme prompts. Each carries a `{word}` placeholder that is filled with a
     * genuinely recurring word, so the question is anchored to something the
     * user actually wrote rather than being generic advice. A prompt with no
     * placeholder would still read fine — and would silently drop the only part
     * that made it personal.
     */
    private val THEMES = listOf(
        "You have written about “{word}” before. What is different about it now?",
        "“{word}” keeps coming up in what you write. What do you make of that?",
        "The word “{word}” appears in several of your entries. Why do you think that is?",
        "You keep returning to “{word}”. Is it unfinished, or is it simply important?"
    )

    private val ONBOARDING = listOf(
        "What brought you here — what were you hoping to keep a record of?",
        "What would you want to be able to find again in a year?",
        "Is there something you keep meaning to write down?"
    )

    /** Below this many entries, the question is an invitation rather than a reference. */
    private const val MIN_ENTRIES_FOR_REFERENCE = 2

    /** Deterministic index for a day, so the question is stable across launches. */
    private fun dayIndex(date: LocalDate, modulus: Int): Int =
        Math.floorMod(date.toEpochDay().toInt(), modulus)

    /**
     * The question for today.
     *
     * Selection order is deliberate: something to reflect on for a journal
     * with real history, a reference to the user's own words next, a theme
     * once one has actually been found, and an invitation only when there is
     * nothing to look back at.
     */
    fun questionFor(snapshot: DiarySnapshot): DiaryQuestion {
        if (snapshot.isEmpty) {
            return DiaryQuestion(ONBOARDING[dayIndex(today(snapshot), ONBOARDING.size)], DiaryQuestion.Kind.ONBOARDING)
        }

        val theme = snapshot.patternThemes()
        if (snapshot.entries.size >= MIN_ENTRIES_FOR_REFERENCE && theme != null && dayIndex(today(snapshot), 2) == 0) {
            return DiaryQuestion(
                THEMES[dayIndex(today(snapshot), THEMES.size)].replace("{word}", theme),
                DiaryQuestion.Kind.THEME
            )
        }

        if (snapshot.entries.size >= MIN_ENTRIES_FOR_REFERENCE) {
            return DiaryQuestion(PAST[dayIndex(today(snapshot), PAST.size)], DiaryQuestion.Kind.PAST_ENTRY)
        }

        return DiaryQuestion(PRESENT[dayIndex(today(snapshot), PRESENT.size)], DiaryQuestion.Kind.PRESENT)
    }

    private fun today(snapshot: DiarySnapshot): LocalDate = LocalDate.ofEpochDay(snapshot.todayEpochDay)

    /**
     * The top recurring word, or `null`.
     *
     * Requires a genuinely recurring word rather than the single most frequent
     * one, because a question built on a word that happened to repeat in one
     * long entry would be pointing at nothing.
     */
    private fun DiarySnapshot.patternThemes(): String? =
        PatternDetector.analyze(this).recurringKeywords.firstOrNull()?.word
}