package com.lifeos.app.core.data

import android.content.Context
import androidx.room.Room
import com.lifeos.app.core.ai.AiProvider
import com.lifeos.app.core.ai.LocalRuleBasedProvider
import com.lifeos.app.core.backup.DataExporter
import com.lifeos.app.core.database.LifeOsDatabase

/**
 * Manual dependency container (spec-adjacent choice documented in ARCHITECTURE.md:
 * no Hilt — one app, one graph, assembled once and passed explicitly).
 *
 * Constructed in [com.lifeos.app.LifeOsApplication] and reached from Compose via
 * [LocalAppContainer].
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val database: LifeOsDatabase by lazy {
        Room.databaseBuilder(appContext, LifeOsDatabase::class.java, LifeOsDatabase.NAME)
            // v1 has no migrations to run. Destructive fallback is deliberately NOT
            // enabled: losing user data on an upgrade is worse than a crash we can
            // fix with a real migration (see SECURITY.md).
            .build()
    }

    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }

    val notes: NoteRepository by lazy { NoteRepository(database) }
    val tracking: TrackingRepositories by lazy { TrackingRepositories(database) }
    val captures: CaptureRepository by lazy { CaptureRepository(appContext, database) }
    val timeline: TimelineSearchRepository by lazy { TimelineSearchRepository(database) }

    /** Dependency-free JSON/text export of the user's own data (no network). */
    val exporter: DataExporter by lazy { DataExporter(database) }

    /**
     * The AI abstraction. Exactly one provider is active; the default is the
     * local, deterministic, rule-based provider — it needs no key, no network and
     * no data leaving the device. A remote provider is intentionally not wired
     * (see SECURITY.md: no embedded API keys).
     */
    val aiProvider: AiProvider by lazy { LocalRuleBasedProvider() }
}
