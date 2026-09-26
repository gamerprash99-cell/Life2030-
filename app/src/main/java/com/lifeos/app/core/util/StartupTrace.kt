package com.lifeos.app.core.util

import android.os.Build
import android.os.Trace

/**
 * Startup profiling for the encrypted-database cold open.
 *
 * Purpose: prove *where* the multi-second splash actually went, on a real
 * device, without shipping a single line of log output. Everything here uses
 * `android.os.Trace`, which the framework compiles down to a no-op unless a
 * tracing session (Perfetto / systrace / `am start -s`) is attached — so the
 * production cost is one boolean check per call site and nothing is ever
 * written to logcat.
 *
 * ```bash
 * # real cold-start breakdown on a device
 * perfetto -o startup.perfetto-trace -t 20s sched freq idle am wm gfx view binder_driver dalvik
 *
 * # platform-level totals
 * adb shell am start -W -S -n com.lifeos.app/.MainActivity   # TotalTime / WaitTime
 * ```
 *
 * Sections emitted:
 * - `lifeos:Application.onCreate`    (sync)  — process start to container built
 * - `lifeos:di.build`                (sync)  — building the DI container
 * - `lifeos:db.open`                 (async) — SQLCipher open end-to-end, the real cost
 * - `lifeos:db.passphrase`           (sync)  — Keystore load + AES/GCM unwrap (inside db.open)
 * - `lifeos:MainActivity.setContent` (sync)  — Activity onCreate to first composition
 * - `lifeos:home.firstFrame`         (sync)  — first Home composition once the DB is ready
 *
 * Async sections never overlap (only one database open happens per process), so
 * a single shared cookie is enough to match begin/end.
 *
 * Tracing is supported from API 29, where `Trace.isEnabled()` — the only cheap
 * way to ask whether a session is attached — was added. Below that every entry
 * point compiles down to a bare call of the wrapped block, which is the same
 * cost as not tracing at all.
 */
object StartupTrace {

    private const val COOKIE = 0x10F5

    /** True while a tracing session is recording. Use this to skip work. */
    fun isEnabled(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()

    /** Starts an async, cross-thread (coroutine-safe) trace section. */
    fun beginAsync(section: String) {
        if (!isEnabled()) return
        Trace.beginAsyncSection(section, COOKIE)
    }

    /** Ends the async section started by [beginAsync]. Safe to call on any thread. */
    fun endAsync(section: String) {
        if (!isEnabled()) return
        Trace.endAsyncSection(section, COOKIE)
    }

    /**
     * Synchronous section for spans that start and end on the same thread
     * (Activity onCreate, first frame). A no-op passthrough when not tracing.
     */
    inline fun <T> section(name: String, block: () -> T): T {
        if (!isEnabled()) return block()
        Trace.beginSection(name)
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }
}
