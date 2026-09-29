import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// The headset talks to the same Supabase project as the phone app.
// quest/local.properties overrides the phone app's local.properties.
val cloudProperties = Properties().apply {
    listOf(rootProject.file("../local.properties"), rootProject.file("local.properties"))
        .filter { it.exists() }
        .forEach { file -> file.inputStream().use { load(it) } }
}

fun cloudProp(key: String): String =
    cloudProperties.getProperty(key, "").orEmpty().replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.hinvr.quest"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hinvr.quest"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${cloudProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${cloudProp("SUPABASE_PUBLISHABLE_KEY")}\"")
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes.add("META-INF/LICENSE")
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// Developer Hub treats a v2-only debug APK as an invalid file. Add the older JAR signature after packaging.
val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
tasks.matching { it.name == "packageDebug" }.configureEach {
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        val apksigner = File(android.sdkDirectory, "build-tools").listFiles()
            ?.map { File(it, "apksigner") }
            ?.filter { it.exists() }
            ?.maxByOrNull { it.parentFile.name }
            ?: return@doLast
        val code = ProcessBuilder(
            listOf(
                apksigner.absolutePath,
                "sign",
                "--ks", debugKeystore.absolutePath,
                "--ks-key-alias", "androiddebugkey",
                "--ks-pass", "pass:android",
                "--key-pass", "pass:android",
                "--min-sdk-version", "21",
                "--v1-signing-enabled=true",
                "--v2-signing-enabled=true",
                "--v3-signing-enabled=true",
                apk.absolutePath,
            ),
        ).inheritIO().start().waitFor()
        if (code != 0) error("apksigner failed with exit code $code")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.zxing.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.meta.spatial.sdk)
    implementation(libs.meta.spatial.sdk.compose)
    implementation(libs.meta.spatial.sdk.toolkit)
    implementation(libs.meta.spatial.sdk.vr)
}
