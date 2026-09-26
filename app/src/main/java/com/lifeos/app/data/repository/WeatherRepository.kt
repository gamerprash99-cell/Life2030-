package com.lifeos.app.data.repository

import com.lifeos.app.domain.model.DiaryAttachment
import com.lifeos.app.domain.model.DiaryWeather

/**
 * Resolves the weather shown beside a diary entry.
 *
 * ## Why this exists but never returns a reading
 *
 * A stock LifeOS build declares **no INTERNET permission** — a deliberate,
 * documented choice (see `AndroidManifest.xml` and `docs/08_SECURITY.md`):
 * diary text, audio, photos and location never leave the device, and the app
 * contains no HTTP client, no API key and no network code at all.
 *
 * Every hosted weather API needs that permission, so obtaining real weather
 * would mean reversing a core privacy guarantee of the product. This build
 * therefore keeps the seam but reports
 * [DiaryWeather.UnavailableReason.NO_SOURCE_OFFLINE_ONLY] instead of
 * fabricating a temperature, an icon or a condition string. The Diary UI
 * renders that as an honest "unavailable" row.
 *
 * To make weather real later, implement [DiaryWeatherSource] against a source
 * the product has explicitly approved, register it in `ServiceLocator`, and the
 * existing composables will start showing [DiaryWeather.Available] with no UI
 * change. No composable, entity or migration is coupled to the source.
 */
interface WeatherRepository {
    suspend fun weatherFor(place: DiaryAttachment.Place?): DiaryWeather
}

/** A real, permitted source of weather observations. No such source ships today. */
interface DiaryWeatherSource {
    /** True when this build is permitted to perform the lookup at all. */
    val isAvailable: Boolean

    /** Human-readable explanation shown when [isAvailable] is false. */
    val unavailableReason: DiaryWeather.UnavailableReason
}

/**
 * The offline-first source: never available, never fabricating.
 *
 * It is a real object rather than a `null` so the dependency graph, the
 * ViewModels and the composables are all exercised exactly as they would be
 * with a working source — only the outcome differs.
 */
class OfflineOnlyWeatherSource : DiaryWeatherSource {
    override val isAvailable: Boolean = false
    override val unavailableReason: DiaryWeather.UnavailableReason =
        DiaryWeather.UnavailableReason.NO_SOURCE_OFFLINE_ONLY
}

class WeatherRepositoryImpl(
    private val source: DiaryWeatherSource
) : WeatherRepository {

    override suspend fun weatherFor(place: DiaryAttachment.Place?): DiaryWeather = when {
        // No place attached -> there is nothing to look up, and nothing to guess.
        place == null -> DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_LOCATION)
        // Guarded by the UI's real permission state; if we ever get here the
        // user has not consented, so we report it instead of looking anything up.
        place.placeName.isEmpty() && place.latitude == 0.0 && place.longitude == 0.0 ->
            DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.LOCATION_UNAVAILABLE)
        !source.isAvailable -> DiaryWeather.Unavailable(source.unavailableReason)
        // A source that is available would perform the lookup here. Until one
        // is approved and registered this branch is unreachable by construction,
        // and we still never return a hardcoded reading.
        else -> DiaryWeather.Unavailable(DiaryWeather.UnavailableReason.NO_SOURCE_OFFLINE_ONLY)
    }
}
