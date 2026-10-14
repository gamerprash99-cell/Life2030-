package com.lifeos.app.core.ai

/**
 * Result of any AiRepository call. Simpler than the old network-backed
 * version — there's no "NoApiKey" case anymore because the LifeOS
 * Intelligence Engine is fully local and always available; the only
 * failure mode is an unexpected exception while reading local data.
 */
sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Error(val message: String) : AiResult()
}
