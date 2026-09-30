package site.xiaozk.dailyfitness.repository.model

/**
 * AI Coach configuration shared between the [settings UI] and the [ai-coach] module.
 *
 * Declared in :repository so that :settings (editing UI) and :ai-coach (consumer)
 * never depend on each other.
 */
data class AiCoachConfig(
    val apiKey: String = "",
    val model: AiCoachModel = AiCoachModel.DeepSeekV4Flash,
    /** Null = use the provider's official endpoint. */
    val baseUrl: String? = null,
    /** Timeout applied to a single LLM request, in seconds. */
    val timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
) {
    val configured: Boolean
        get() = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS: Int = 15
        const val MIN_TIMEOUT_SECONDS: Int = 5
        const val MAX_TIMEOUT_SECONDS: Int = 120
    }
}

enum class AiCoachModel(val modelId: String) {
    DeepSeekV4Flash("deepseek-v4-flash"),
    DeepSeekV4Pro("deepseek-v4-pro"),
}
