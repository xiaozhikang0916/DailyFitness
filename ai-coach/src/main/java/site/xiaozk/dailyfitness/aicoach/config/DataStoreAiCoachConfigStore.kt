package site.xiaozk.dailyfitness.aicoach.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.AiCoachModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed [IAiCoachConfigStore].
 *
 * Android-free: the [DataStore] instance is injected; acquiring it from a
 * Context is the host's job (see the `:app` AiCoachDataStoreModule binding).
 *
 * The API key is stored in plain DataStore preferences (v1). The host excludes
 * `datastore/ai_coach_config.preferences_pb` from backup / device transfer
 * (see the app's backup_rules.xml + data_extraction_rules.xml, M3.4).
 */
@Singleton
class DataStoreAiCoachConfigStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : IAiCoachConfigStore {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
        val BASE_URL = stringPreferencesKey("base_url")
    }

    override fun observe(): Flow<AiCoachConfig> = dataStore.data.map { prefs ->
        AiCoachConfig(
            apiKey = prefs[Keys.API_KEY] ?: "",
            model = prefs[Keys.MODEL]?.let { raw ->
                runCatching { AiCoachModel.valueOf(raw) }.getOrNull()
            } ?: AiCoachModel.DeepSeekV4Flash,
            baseUrl = prefs[Keys.BASE_URL],
        )
    }

    override suspend fun save(config: AiCoachConfig) {
        val baseUrl = config.baseUrl
        dataStore.edit { prefs ->
            prefs[Keys.API_KEY] = config.apiKey
            prefs[Keys.MODEL] = config.model.name
            if (baseUrl.isNullOrBlank()) {
                prefs.remove(Keys.BASE_URL)
            } else {
                prefs[Keys.BASE_URL] = baseUrl.trimEnd('/')
            }
        }
    }
}
