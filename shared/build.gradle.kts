import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            linkerOpts.add("-lsqlite3")
            export(libs.room.runtime)
            export(libs.sqlite)
            export(libs.sqlite.bundled)
            export(libs.sqlite.framework)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Kotlinx Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.logging)

            // Room + SQLite (api so iOS framework exports sqlite extensions such as Statement.use)
            api(libs.room.runtime)
            api(libs.sqlite)
            api(libs.sqlite.bundled)
            api(libs.sqlite.framework)
        }

        androidMain.dependencies {
            // Ktor Client - OkHttp for Android
            implementation(libs.ktor.client.okhttp)

            // Kotlinx Coroutines Android
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.room.ktx)
        }

        iosMain.dependencies {
            // Ktor Client - Darwin for iOS
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqlite.framework)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
}

android {
    namespace = "com.droidcon.global.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

