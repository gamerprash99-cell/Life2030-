package com.lifeos.app.domain.usecase

import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.repository.DiaryRepository
import com.lifeos.app.domain.intelligence.DiaryAnalyzer
import com.lifeos.app.domain.intelligence.DiaryInsights
import com.lifeos.app.domain.intelligence.WritingStreak
import com.lifeos.app.domain.model.DiaryAttachments
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns the rows already on this device into [DiaryInsights].
 *
 * Reads the whole journal, so it runs off the main thread: [analyze] does
 * enough string work over every entry that running it inline would be a
 * dropped-frame bug on every save.
 *
 * There is no network call to make and no cache to keep warm, because the
 * source is the local database and the analysis is deterministic — the same
 * journal always yields the same figures, so recomputing on each emission is
 * both correct and cheap enough at this size.
 *
 * The clock is injected as [today] so the results are testable: every relative
 * date in the output ("is the streak alive", "the last seven days") derives
 * from this one value, so the analyzers can never disagree with each other
 * about where today is.
 */
class GetDiaryInsightsUseCase(
    private val diaryRepository: DiaryRepository,
    private val compute: CoroutineDispatcher = Dispatchers.Default,
    private val today: () -> Long = { DateTimeUtils.today().toEpochDay() }
) {

    suspend operator fun invoke(): DiaryAnalysisResult = withContext(compute) {
        val snapshot = DiaryAnalyzer.snapshotOf(
            entries = diaryRepository.getAll().map { row ->
                DiaryAnalyzer.SnapshotRow(
                    id = row.id,
                    title = row.title,
                    content = row.content,
                    mood = row.mood,
                    tagsCsv = row.tagsCsv,
                    dateEpochDay = row.dateEpochDay,
                    timeMinutes = row.timeMinutes,
                    isFavorite = row.isFavorite,
                    // Decoding attachments here rather than in the engine is
                    // deliberate: `attachmentsJson` is an encoding the data layer
                    // owns, and the engine only ever needs the number.
                    attachmentCount = DiaryAttachments.decode(row.attachmentsJson).size
                )
            },
            todayEpochDay = today()
        )
        DiaryAnalysisResult(
            insights = DiaryAnalyzer.analyze(snapshot),
            streak = DiaryAnalyzer.streak(snapshot)
        )
    }
}

/**
 * Insights and the streak, read from the same snapshot so they cannot disagree.
 *
 * Both come back together rather than as two use cases the screen calls in
 * sequence: a save landing between two reads would otherwise let the streak say
 * 6 days while the insights band said 7 entries.
 */
data class DiaryAnalysisResult(
    val insights: DiaryInsights,
    val streak: WritingStreak
)