package site.xiaozk.dailyfitness.aicoach.prompt

/**
 * Whitespace normalisation used when matching LLM-returned names back to the
 * user's action library (trim + collapse inner whitespace runs).
 */
fun String.normalizeForMatch(): String = trim().replace(Regex("\\s+"), " ")
