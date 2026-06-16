# Keep WebView JavaScript interface names
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Shortcut manager
-keep class androidx.core.content.pm.** { *; }

# Keep our app classes
-keep class com.weblauncher.app.** { *; }
