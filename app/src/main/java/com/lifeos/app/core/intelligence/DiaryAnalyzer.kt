package com.lifeos.app.core.intelligence

import com.lifeos.app.data.db.entities.DiaryEntity

/** Combines MoodAnalyzer + KeywordExtractor into a per-entry or multi-entry diary analysis. */
object DiaryAnalyzer {

    fun analyzeEntry(entry: DiaryEntity): DiaryAnalysis {
        val mood = MoodAnalyzer.analyze(entry.content)
        val keywords = KeywordExtractor.extract(entry.content)
        val wordCount = entry.content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        return DiaryAnalysis(entry.id, mood, keywords, wordCount)
    }

    /** Average mood across a set of entries (e.g. this week), for trend/correlation use. */
    fun averageMood(entries: List<DiaryEntity>): Double {
        if (entries.isEmpty()) return 0.0
        return entries.map { MoodAnalyzer.numericValue(MoodAnalyzer.analyze(it.content).mood) }.average()
    }

    fun topKeywordsAcross(entries: List<DiaryEntity>, max: Int = 8): List<KeywordResult> =
        KeywordExtractor.extractAcross(entries.map { it.content }, max)
}
