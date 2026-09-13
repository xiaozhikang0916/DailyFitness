package site.xiaozk.dailyfitness.aicoach.engine

/**
 * Supplies the BCP-47 language tag (e.g. `"zh-CN"`, `"en-US"`) that coach replies
 * must be written in.
 *
 * The value is resolved once when the single engine instance is created, so a
 * locale change only takes effect after the process is recreated - acceptable for
 * the current requirement (no reactive rebuild).
 */
fun interface CoachLocaleProvider {
    fun languageTag(): String
}
