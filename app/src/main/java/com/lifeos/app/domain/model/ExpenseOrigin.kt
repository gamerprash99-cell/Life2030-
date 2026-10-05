package com.lifeos.app.domain.model

import com.lifeos.app.data.db.entities.ExpenseEntity

/**
 * Marks an expense that LifeOS created itself from a payment notification.
 *
 * The marker lives in the existing `tagsCsv` column rather than in a new column
 * or a new table, which is what keeps this feature from needing a Room
 * migration at all — the schema, the exported schema JSON and every installed
 * database are untouched. It also travels with the row through backup and
 * restore, so a restored expense still reads as automatically detected.
 *
 * It is deliberately a *tag* and not a prefix check on [ExpenseEntity.id]: the
 * id prefix belongs to the fingerprint (see
 * [com.lifeos.app.domain.usecase.CaptureTransactionUseCase]) and is an
 * implementation detail of deduplication. Two sources of truth for the same
 * fact would eventually disagree.
 */
const val AUTO_CAPTURED_TAG = "auto-captured"

/** True for an expense LifeOS filed itself from a payment notification. */
fun ExpenseEntity.isAutomaticallyCaptured(): Boolean =
    tagsCsv.split(',').any { it.trim() == AUTO_CAPTURED_TAG }
