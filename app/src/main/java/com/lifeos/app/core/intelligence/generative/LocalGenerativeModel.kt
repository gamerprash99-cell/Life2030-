package com.lifeos.app.core.intelligence.generative

/**
 * Contract for a genuinely local generative model.
 *
 * Implementations must keep inference on-device. The contract deliberately
 * knows nothing about Room, networking, UI, or API keys so a future model
 * runtime can be added without changing LifeOS feature layers.
 */
interface LocalGenerativeModel {
    val info: LocalModelInfo

    /** Returns generated text or throws a model-specific inference exception. */
    suspend fun generate(request: GenerationRequest): String
}

data class LocalModelInfo(
    val id: String,
    val displayName: String,
    val version: String,
    val parameterCount: Long? = null,
    val quantization: String? = null,
    val bundledSizeBytes: Long = 0L,
    val isGenerative: Boolean = true
)

data class GenerationRequest(
    val systemInstruction: String,
    val prompt: String,
    val maxTokens: Int = 256,
    val temperature: Float = 0.2f
) {
    init {
        require(maxTokens in 1..4096) { "maxTokens must be between 1 and 4096" }
        require(temperature in 0f..2f) { "temperature must be between 0 and 2" }
    }
}
