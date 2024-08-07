plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "site.xiaozk.dailyfitness"
    compileSdk = 34

    defaultConfig {
        applicationId = "site.xiaozk.dailyfitness"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    signingConfigs {
        create("release") {
            storeFile = file("${System.getenv("KEYSTORE_FILE") ?: "release.keystore"}")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_ALIAS_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }
    kotlinOptions {
        jvmTarget = libs.versions.java.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    applicationVariants.configureEach {
        outputs.configureEach {
            val outputFile = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            val fileName = "DailyFitness-${name}-${versionName}.apk"
            outputFile.outputFileName = fileName

        }

    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity)
    implementation(libs.android.hilt.lib)
    implementation(libs.datetime)
    implementation(libs.serializationx.json)
    kapt(libs.android.hilt.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidx.compose)
    implementation(libs.bundles.accompanist)
    implementation(libs.androidx.navigation.compose.core)
    implementation(libs.androidx.navigation.compose.hilt)
    implementation(project(":repository"))
    implementation(project(":database"))
    implementation(project(":calendar"))
    implementation(project(":chart"))
}