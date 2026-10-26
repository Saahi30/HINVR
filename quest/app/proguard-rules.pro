# HINVR Quest release shrinking.
# The Spatial SDK ships its own consumer rules (DoNotStrip / DoNotStripNative); these cover the app.

# The YouTube player calls back into Kotlin by method name.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Systems and panels are registered by instance, but the SDK logs and looks systems up by class.
-keep class * extends com.meta.spatial.core.SystemBase

# org.json and the platform WebView are part of Android; nothing extra to keep.
-dontwarn org.jetbrains.annotations.**
