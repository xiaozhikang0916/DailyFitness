@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.serialization)
}

android {
    namespace = "site.xiaozk.dailyfitness.aicoach"
    compileSdk = 37

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

// Optional external API key, reserved for real-LLM tests only (none yet). Domain
// tests use a fake LLM seam and run without any key:
// ./gradlew :ai-coach:testDebugUnitTest -Pdeepseek.apiKey=sk-...
// (falls back to the DEEPSEEK_API_KEY env var)
val deepseekApiKey = providers.gradleProperty("deepseek.apiKey").getOrElse("")

android {
    testOptions {
        unitTests.all {
            it.systemProperty("deepseek.apiKey", deepseekApiKey)
        }
    }
}

dependencies {
    ksp(libs.android.hilt.compiler)
    implementation(libs.android.hilt.lib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutine.core)
    implementation(libs.serializationx.json)
    implementation(libs.datetime)
    implementation(libs.androidx.datastore.preferences.core)

    implementation(libs.koog.agents)
    implementation(libs.koog.agents.additions)

    implementation(project(":repository"))

    testImplementation(libs.junit)
    testImplementation(libs.coroutine.test)
    // Real-LLM integration tests only (offline domain tests use fakes):
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.okhttp)
}
