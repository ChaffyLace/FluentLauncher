# ==============================================================================
# FLUTIS LAUNCHER - E-COMMERCE & PRODUCTION PROGUARD RULES
# ==============================================================================
# Author: Gemini AI Collaboration
# Description: Hardened configuration for AGSL Shaders, Room Persistence,
#              and Google Play Billing protection.
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. CORE KOTLIN & REFLECTION PROTECTION
# ------------------------------------------------------------------------------
# Preserves metadata needed for Kotlin's advanced features and reflection.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, SourceFile, LineNumberTable
-keep class kotlin.coroutines.Continuation { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.reflect.**

# ------------------------------------------------------------------------------
# 2. JETPACK COMPOSE & AGSL SHADERS (Critical for Mica Effect)
# ------------------------------------------------------------------------------
# Compose relies on specific naming for recomposition optimization.
# AGSL (Shaders) variables must not be renamed to ensure UI rendering.
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class android.graphics.RuntimeShader { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# ------------------------------------------------------------------------------
# 3. ROOM PERSISTENCE (Dashboard Tiles)
# ------------------------------------------------------------------------------
# Preventing Room from losing track of SQL tables and columns.
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-keep @androidx.room.Dao class * { *; }
-keep class androidx.room.Entity
-keep class androidx.room.Dao
-keep class androidx.room.Query
-keep class androidx.room.Transaction
-keep class androidx.room.Database

# Mandatory: Keep your specific models to prevent mapping errors
-keep class com.example.fluentui.ui.database.** { *; }

# ------------------------------------------------------------------------------
# 4. DATASTORE, GSON & PREFERENCES (Settings Persistence)
# ------------------------------------------------------------------------------
# If these are obfuscated, user settings will reset on every update.
-keep class com.example.fluentui.ui.settings.SettingsState { *; }
-keepattributes Signature
-keepnames class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements com.google.gson.TypeAdapterFactory
-keep public class * implements com.google.gson.JsonSerializer
-keep public class * implements com.google.gson.JsonDeserializer
-dontwarn com.google.gson.**

# ------------------------------------------------------------------------------
# 5. MONETIZATION & GOOGLE SERVICES
# ------------------------------------------------------------------------------
# Keeps Billing library intact for "Tips/Donations" functionality.
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ------------------------------------------------------------------------------
# 6. NETWORKING (Retrofit/Coil/OkHttp)
# ------------------------------------------------------------------------------
# Essential for Web Search Affiliation and Icon Loading.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep class coil.** { *; }
-dontwarn coil.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ------------------------------------------------------------------------------
# 7. PRODUCTION SECURITY OPTIMIZATIONS
# ------------------------------------------------------------------------------
# Removes all debug logs from the final APK to prevent reverse engineering
# of your app's internal logic and security.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Maintain line numbers in stack traces for Play Store Console bug reports,
# but obfuscate the actual source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile