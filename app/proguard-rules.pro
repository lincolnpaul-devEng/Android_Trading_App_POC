# ==============================================================================
# Security, Obfuscation & Shrinking ProGuard / R8 Rules for Trading App
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Code Obfuscation & Optimization
# ------------------------------------------------------------------------------
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# Keep source file attributes for crash reporting line numbers without exposing paths
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Keep custom annotations and runtime annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Strip all Android Log calls in production for security (no leakage of sensitive financial data)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# ------------------------------------------------------------------------------
# 2. Kotlin & Coroutines
# ------------------------------------------------------------------------------
-dontwarn kotlin.Unit
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ------------------------------------------------------------------------------
# 3. Jetpack Compose
# ------------------------------------------------------------------------------
-keep class androidx.compose.ui.platform.** { *; }
-keepclassmembers class * implements androidx.compose.runtime.State { *; }

# ------------------------------------------------------------------------------
# 4. Rive Android Runtime (Required to preserve native JNI bindings & animations)
# ------------------------------------------------------------------------------
-keep class app.rive.runtime.kotlin.** { *; }
-keepclassmembers class app.rive.runtime.kotlin.** { *; }
-keep class app.rive.runtime.kotlin.core.** { *; }
-keepclassmembers class app.rive.runtime.kotlin.core.** { *; }
-dontwarn app.rive.runtime.kotlin.**

# ------------------------------------------------------------------------------
# 5. Gson & Serialization (Preserve serialized entity DTOs & data models)
# ------------------------------------------------------------------------------
-keepattributes EnclosingMethod
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}
# Keep all data models that are serialized to/from JSON or Room
-keep class com.trading.app.data.remote.model.** { *; }
-keep class com.trading.app.data.remote.quiz.** { *; }
-keep class com.trading.app.domain.quiz.model.** { *; }
-keep class com.trading.app.engine.core.** { *; }
-keep class com.trading.app.engine.market.** { *; }
-keep class com.trading.app.engine.trading.** { *; }
-keep class com.trading.app.engine.risk.** { *; }
-keep class com.trading.app.engine.portfolio.** { *; }
-keep class com.trading.app.engine.assessment.** { *; }

# ------------------------------------------------------------------------------
# 6. Room Database Persistence
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
    @androidx.room.Query *;
}

# ------------------------------------------------------------------------------
# 7. Retrofit & OkHttp
# ------------------------------------------------------------------------------
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-keepattributes Exceptions
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ------------------------------------------------------------------------------
# 8. Dagger / Hilt Dependency Injection
# ------------------------------------------------------------------------------
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class dagger.hilt.android.internal.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject *;
}

# ------------------------------------------------------------------------------
# 9. Firebase Cloud Messaging
# ------------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
