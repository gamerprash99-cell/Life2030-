package com.lifeos.app.domain.model

/**
 * Weather attached to a diary entry, as an explicit state machine.
 *
 * LifeOS is offline-first and privacy-first: it ships **no INTERNET
 * permission**, no HTTP client and no API key, so there is no weather source
 * the app is permitted to use. Rather than invent a temperature or a condition
 * (which would be a lie rendered in the UI), the Diary renders these states
 * honestly. The seam exists so a real source can be dropped in later without
 * touching a single composable — see `WeatherRepository`.
 */
sealed interface DiaryWeather {

    /** A real reading. Only ever constructed from an actual measurement. */
    data class Available(
        val temperatureCelsius: Double,
        val condition: String,
        val observedAtEpochMillis: Long
    ) : DiaryWeather

    /** Why no reading can be shown right now. [reason] is user-facing copy. */
    data class Unavailable(val reason: UnavailableReason) : DiaryWeather

    enum class UnavailableReason {
        /** No location was ever attached, so there is nothing to look up. */
        NO_LOCATION,

        /** The user has not granted location permission. */
        PERMISSION_DENIED,

        /** Permission was denied permanently; only Settings can undo it. */
        PERMISSION_PERMANENTLY_DENIED,

        /** Location providers are off, or no fix could be obtained. */
        LOCATION_UNAVAILABLE,

        /**
         * No weather source is configured for this build. This is the state a
         * stock offline-first build shows, and it is deliberate: LifeOS does not
         * hold the INTERNET permission that any hosted weather API would need.
         */
        NO_SOURCE_OFFLINE_ONLY
    }
}

/** True when a real reading exists (i.e. the UI may show a temperature). */
val DiaryWeather.hasReading: Boolean get() = this is DiaryWeather.Available
