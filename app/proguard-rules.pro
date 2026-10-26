# Supabase Kotlin SDK (Ktor + kotlinx.serialization).
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.ktor.**

# kotlinx.serialization ships its own rules; these keep the app's models
# findable when a serializer is looked up by type at runtime (Postgrest decode).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepclassmembers @kotlinx.serialization.Serializable class com.hinvr.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hinvr.app.**$$serializer { *; }
-keepclasseswithmembers class com.hinvr.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor resolves its engine through ServiceLoader on Android.
-keep class io.ktor.client.engine.okhttp.OkHttpEngineContainer { *; }
-keepnames class io.ktor.** { *; }
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**

# Coil 3 ships consumer rules. Nothing extra for the OkHttp fetcher.

# The player WebView exposes no JavaScript interface today. If one is added,
# annotate its methods with @JavascriptInterface; this keeps them.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Firebase Messaging service is named in the manifest; R8 keeps it from there.
