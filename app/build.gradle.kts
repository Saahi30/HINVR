import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase needs app/google-services.json. Without it the app still builds and
// push stays off (see push/Push.kt), so local and CI builds work without secrets.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String, default: String = ""): String {
    val raw = localProperties.getProperty(key, default).orEmpty()
    return raw.replace("\\", "\\\\").replace("\"", "\\\"")
}

// Gradle property (-Phinvr.versionCode=42), then environment, then local.properties.
fun buildSetting(key: String, env: String, default: String = ""): String =
    (findProperty(key) as String?)
        ?: System.getenv(env)
        ?: localProperties.getProperty(key)
        ?: default

// Release signing never lives in the repo: local.properties (gitignored) on a
// workstation, environment variables in CI. Missing values leave it unsigned.
fun signingSetting(key: String): String? =
    localProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingSetting("HINVR_KEYSTORE_PATH")
val releaseStorePassword = signingSetting("HINVR_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingSetting("HINVR_KEY_ALIAS")
val releaseKeyPassword = signingSetting("HINVR_KEY_PASSWORD")
val canSignRelease = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it != null } && rootProject.file(releaseStoreFile!!).exists()

android {
    namespace = "com.hinvr.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hinvr.app"
        minSdk = 26
        targetSdk = 36
        versionCode = buildSetting("hinvr.versionCode", "HINVR_VERSION_CODE", "1").toInt()
        versionName = buildSetting("hinvr.versionName", "HINVR_VERSION_NAME", "0.1.0")
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
            "String",
            "MEMBERSHIP_URL",
            "\"${localProp("MEMBERSHIP_URL", "https://hinvr-membership.vercel.app")}\"",
        )
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Drawables are WebP; nothing left for PNG crunching to do.
            isCrunchPngs = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (canSignRelease) signingConfig = signingConfigs.getByName("release")
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

    testOptions {
        unitTests.isReturnDefaultValues = true
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
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.zxing.core)
    implementation(libs.haze)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
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
