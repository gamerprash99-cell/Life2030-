package com.lifeos.app.core.ai

import kotlinx.serialization.Serializable

/** Note-level AI actions — Section 8. */
enum class NoteAiAction(val label: String, val instruction: String) {
    SUMMARIZE("Summarize", "Summarize the following note in 2-4 concise sentences."),
    GENERATE_TITLE("Generate title", "Write a short, specific title (max 8 words) for the following note. Reply with ONLY the title, no punctuation around it."),
    ORGANIZE("Organize text", "Reorganize the following messy text into clear, well-structured paragraphs or bullet points, preserving all information."),
    REWRITE("Rewrite", "Rewrite the following text to be clearer and more polished, preserving its meaning and tone."),
    IMPROVE_GRAMMAR("Improve grammar", "Fix grammar and spelling in the following text without changing its meaning or tone."),
    MAKE_SHORTER("Make shorter", "Make the following text noticeably shorter while keeping all key information."),
    MAKE_LONGER("Make longer", "Expand the following text with more helpful detail, keeping the same tone."),
    EXTRACT_POINTS("Extract important points", "Extract the most important points from the following text as a short bulleted list."),
    CREATE_CHECKLIST("Create checklist", "Convert the actionable items in the following text into a checklist, one item per line, no numbering."),
    GENERATE_IDEAS("Generate ideas", "Based on the following text, suggest 3-5 relevant follow-up ideas."),
    EXPLAIN("Explain content", "Explain the following content simply, as if to someone unfamiliar with the topic."),
    STUDY_QUESTIONS("Create study questions", "Write 5 study questions based on the following notes, to help the user test their understanding."),
}

@Serializable
data class ExtractedTask(
    val title: String,
    val dueDateHint: String? = null // free-text hint like "Friday" or "tomorrow"; resolved by the caller
)

@Serializable
data class ChatMessage(
    val role: String,   // "user" | "assistant"
    val content: String
)

/** A structured card the AI assistant can attach to a reply — Section 55. */
sealed class AiResponseCard {
    data class ExpenseSummary(val category: String, val amount: Double, val percentOfMonth: Int) : AiResponseCard()
    data class HabitSummary(val habitName: String, val completed: Int, val total: Int, val percent: Int) : AiResponseCard()
    data class TaskExtraction(val tasks: List<ExtractedTask>) : AiResponseCard()
}
