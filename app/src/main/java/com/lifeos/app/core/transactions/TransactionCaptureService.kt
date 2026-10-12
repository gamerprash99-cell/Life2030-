package com.lifeos.app.core.transactions

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.lifeos.app.core.di.ServiceLocator
import com.lifeos.app.core.util.NotificationAccess
import com.lifeos.app.domain.intelligence.TransactionParser
import com.lifeos.app.domain.model.TransactionCandidate
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Turns payment notifications into expenses, on the device, with no network.
 *
 * This is the one component that reads something the user did not type into
 * LifeOS, so its design is mostly about what it refuses to do:
 *
 *  - **It does nothing until asked.** Notification-listener access is a system
 *    grant the user makes on the settings screen, and [ServiceLocator]'s
 *    `autoCaptureExpenses` flag is checked on every notification, so turning the
 *    feature off takes effect on the next notification rather than the next
 *    process restart.
 *  - **It keeps nothing.** Only the two text fields the parser needs are read,
 *    each capped, and neither the text nor anything derived from it is logged.
 *    What reaches the database is the parsed amount, merchant, category, method
 *    and time — the same fields the add-expense sheet writes.
 *  - **It filters before it parses.** [TransactionParser.looksFinancial] needs
 *    both a currency marker and a movement word, so ordinary notifications are
 *    rejected on two substring tests rather than a dozen regexes.
 *  - **It is not the decision-maker.** Parsing, confidence, duplicate detection
 *    and categorisation all live in
 *    [com.lifeos.app.domain.usecase.CaptureTransactionUseCase]; this class only
 *    extracts text and hands it over. That is what keeps the rules testable
 *    without a device.
 *
 * The service is declared `exported` because the platform requires it, and bound
 * only by `BIND_NOTIFICATION_LISTENER_SERVICE` — see the manifest. It cannot be
 * started by another app.
 */
class TransactionCaptureService : NotificationListenerService() {

    private val scope = CoroutineScope(
        SupervisorJob() +
            // Parsing, the settings read and the database write are all blocking
            // work, and none of it may run on the main thread: this callback is
            // invoked on the main thread by the platform.
            Dispatchers.Default +
            // A failure on one notification must not tear down the listener for
            // the next one.
            CoroutineExceptionHandler { _, _ -> }
    )

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName) return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        // A group summary is the collapsed "3 new messages" row, not a message.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val body = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()

        val candidate = TransactionCandidate(
            title = title?.take(MAX_TEXT_CHARS),
            body = body?.take(MAX_TEXT_CHARS),
            postedAtEpochMillis = sbn.postTime
        )
        // The cheap gate runs on the caller's thread: it is a length check and
        // two substring tests, and it keeps every non-financial notification out
        // of the coroutine queue entirely.
        if (!TransactionParser.looksFinancial(candidate.combinedText)) return

        scope.launch { record(candidate) }
    }

    /**
     * Notification access can be revoked while the app runs, and the feature
     * toggle can be turned off without the service ever being unbound — so both
     * are re-read here rather than cached at bind time.
     */
    private suspend fun record(candidate: TransactionCandidate) {
        val locator = ServiceLocator.get(applicationContext)
        if (!locator.settingsStore.autoCaptureExpenses.first()) return
        if (!NotificationAccess.isGranted(applicationContext)) return
        locator.captureTransactionUseCase(candidate)
    }

    private companion object {
        /**
         * Cap per field. A payment notification is a sentence or two; anything
         * past this is a promo or a receipt, and a shorter input is both cheaper
         * to parse and less revealing.
         */
        const val MAX_TEXT_CHARS = 400
    }
}