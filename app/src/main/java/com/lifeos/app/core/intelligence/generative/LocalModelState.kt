package com.lifeos.app.core.intelligence.generative

/** Lifecycle exposed to the rest of LifeOS without leaking runtime details. */
sealed interface LocalModelState {
    data object Unavailable : LocalModelState
    data object Loading : LocalModelState
    data class Ready(val info: LocalModelInfo) : LocalModelState
    data class Failed(val message: String) : LocalModelState
}

/** Provider-independent result used by the intelligence facade. */
sealed interface GenerationResult {
    data class Success(val text: String, val model: LocalModelInfo) : GenerationResult
    data class Unavailable(val reason: String) : GenerationResult
    data class Failed(val reason: String) : GenerationResult
}
