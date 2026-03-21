import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

compose.resources {
    packageOfResClass = "com.quranapp"
    generateResClass = always
    publicResClass = true
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {

        // ── Shared (all platforms) ─────────────────────────────────────────
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.koin)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.transitions)

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Database
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Network (chatbot API only)
            implementation(libs.ktor.core)
            implementation(libs.ktor.content.neg)
            implementation(libs.ktor.serialization)
            implementation(libs.ktor.logging)

            // Serialization
            implementation(libs.serialization.json)

            // Async
            implementation(libs.coroutines.core)

            // Settings
            implementation(libs.mp.settings)
            implementation(libs.mp.settings.coroutines)

            // Prayer times
            implementation(libs.adhan)
        }

        // ── Shared tests ───────────────────────────────────────────────────
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
            implementation(libs.mockk)
            implementation(libs.sqldelight.sqlite)   // in-memory driver for tests
        }

        // ── Android ────────────────────────────────────────────────────────
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.ktor.okhttp)
            implementation(libs.sqldelight.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation("com.google.android.gms:play-services-location:21.2.0")
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.compose.ui.test)
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.test.runner)
            }
        }

        // ── iOS ────────────────────────────────────────────────────────────
        iosMain.dependencies {
            implementation(libs.ktor.darwin)
            implementation(libs.sqldelight.native)
        }
    }
}

android {
    namespace = "com.quranapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quranapp"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].assets.srcDirs("src/androidMain/assets")
}

sqldelight {
    databases {
        create("QuranDatabase") {
            packageName.set("com.quranapp.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:2.0.2")
        }
    }
}
