package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig

/**
 * Persistence contract for [AiCoachConfig].
 *
 * Implementation lives in :ai-coach (DataStore); the editing UI (:settings, later)
 * only depends on this interface from :repository.
 */
interface IAiCoachConfigStore {
    fun observe(): Flow<AiCoachConfig>
    suspend fun save(config: AiCoachConfig)
}
