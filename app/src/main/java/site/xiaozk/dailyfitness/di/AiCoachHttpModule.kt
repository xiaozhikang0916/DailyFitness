package site.xiaozk.dailyfitness.di

import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.ktor.KtorKoogHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

/**
 * Host-side HTTP transport wiring for :ai-coach.
 *
 * The concrete [KoogHttpClient.Factory] (ktor + OkHttp engine, no SSE) is decided
 * here and injected into the executor; :ai-coach stays free of concrete engine
 * imports. Note: an explicit factory is required anyway - koog's ServiceLoader
 * default does not resolve in Android local unit tests (AAR META-INF/services
 * are not merged) and would be ambiguous on a device with several engines.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiCoachHttpModule {

    @Provides
    @Singleton
    fun provideKoogHttpClientFactory(): KoogHttpClient.Factory =
        KtorKoogHttpClient.Factory(
            baseClient = HttpClient(OkHttp),
            withSse = false,
        )
}
