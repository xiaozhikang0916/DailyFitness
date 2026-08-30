package site.xiaozk.dailyfitness.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal persisted session state. Everything else (set counts, current action)
 * is derived from the repository, so only the two fields below survive
 * process death.
 */
data class SessionMeta(
    val active: Boolean = false,
    val startedAt: Instant? = null,
)

interface SessionStore {
    fun observe(): Flow<SessionMeta>
    suspend fun setActive(meta: SessionMeta)
}

private val Context.sessionDataStore by preferencesDataStore(name = "workout_session")

@Singleton
class DataStoreSessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionStore {

    private object Keys {
        val ACTIVE = booleanPreferencesKey("active")
        val STARTED_AT = longPreferencesKey("started_at")
    }

    override fun observe(): Flow<SessionMeta> = context.sessionDataStore.data.map { prefs ->
        SessionMeta(
            active = prefs[Keys.ACTIVE] ?: false,
            startedAt = prefs[Keys.STARTED_AT]?.let(Instant::fromEpochMilliseconds),
        )
    }

    override suspend fun setActive(meta: SessionMeta) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.ACTIVE] = meta.active
            if (meta.startedAt != null) {
                prefs[Keys.STARTED_AT] = meta.startedAt.toEpochMilliseconds()
            } else {
                prefs.remove(Keys.STARTED_AT)
            }
        }
    }
}
