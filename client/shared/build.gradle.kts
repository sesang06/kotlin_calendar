import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.wire)
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }

    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            implementation(libs.compose.material.icons.core)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.wire.runtime)
            implementation(libs.wire.grpc.client)
            api(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(
                npm(
                    "@js-joda/timezone",
                    libs.versions.js.joda.timezone
                        .get(),
                ),
            )
        }
    }
}

// 서버와 같은 proto/ 를 Wire로 Kotlin Multiplatform 코드로 생성한다 (proto-api 스킬).
wire {
    sourcePath {
        srcDir(rootProject.file("proto"))
    }
    kotlin {
        rpcRole = "client"
    }
}

android {
    namespace = "com.kotlincalendar.client.shared"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
