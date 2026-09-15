package com.lifeos.app.core.intelligence

/**
 * Plain arithmetic trend calculation: compares a metric's value in the
 * current period against the previous period of equal length. This is the
 * "trend detection" the product spec asks for — no forecasting model,
 * just a percentage-change comparison, which is honest and explainable.
 */
object TrendAnalyzer {

    fun compare(metric: String, current: Double, previous: Double): TrendResult {
        val percentChange = if (previous != 0.0) {
            (((current - previous) / previous) * 100).toInt()
        } else if (current == 0.0) {
            0
        } else {
            null
        }

        val direction = when {
            percentChange == null -> TrendDirection.UNKNOWN
            percentChange > 5 -> TrendDirection.UP
            percentChange < -5 -> TrendDirection.DOWN
            else -> TrendDirection.FLAT
        }

        return TrendResult(
            metric = metric,
            currentPeriodValue = current,
            previousPeriodValue = previous,
            percentChange = percentChange,
            direction = direction
        )
    }
}
