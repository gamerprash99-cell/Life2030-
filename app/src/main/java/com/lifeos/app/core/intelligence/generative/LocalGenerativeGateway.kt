package com.lifeos.app.core.intelligence.generative

/**
 * Small orchestration layer around the optional local model.
 *
 * Phase 19 intentionally ships no model. Until a verified on-device runtime
 * is added, requests fail closed instead of silently falling back to a cloud
 * service or pretending that deterministic rules are generative AI.
 */
class LocalGenerativeGateway(
    private val model: LocalGenerativeModel? = null
) {
    val state: LocalModelState
        get() = model?.let { LocalModelState.Ready(it.info) } ?: LocalModelState.Unavailable

    suspend fun generate(request: GenerationRequest): GenerationResult {
        val localModel = model
            ?: return GenerationResult.Unavailable(
                "No local generative model is bundled in this LifeOS build."
            )

        return try {
            GenerationResult.Success(
                text = localModel.generate(request),
                model = localModel.info
            )
        } catch (t: Throwable) {
            GenerationResult.Failed(
                t.message?.takeIf { it.isNotBlank() } ?: "Local model inference failed."
            )
        }
    }
}
