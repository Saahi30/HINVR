import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String, default: String = ""): String {
    val raw = localProperties.getProperty(key, default).orEmpty()
    return raw.replace("\\", "\\\\").replace("\"", "\\\"")
}

android {
    namespace = "com.hinvr.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hinvr.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = true
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProp("SUPABASE_URL")}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProp("SUPABASE_PUBLISHABLE_KEY")}\"",
        )
        buildConfigField(
            "boolean",
            "MOCK_PHONE_OTP",
            localProp("MOCK_PHONE_OTP", "true").ifBlank { "true" },
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Android Studio 2025.3 looks for intermediates/apk/debug/app-debug.apk.
// AGP 9 writes the APK to outputs/apk/debug. Copy so Run/Debug can install.
afterEvaluate {
    tasks.matching { it.name == "packageDebug" }.configureEach {
        doLast {
            val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
            val studioApk = layout.buildDirectory.file("intermediates/apk/debug/app-debug.apk").get().asFile
            if (apk.exists()) {
                studioApk.parentFile.mkdirs()
                apk.copyTo(studioApk, overwrite = true)
            }
        }
    }
}
