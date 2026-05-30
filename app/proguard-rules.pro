# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# AutomaticFinances - Production ProGuard Rules
# Optimized for security and performance

# Keep debugging information for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep line numbers for better crash reports
-keepattributes *Annotation*,Signature,Exception

# ===============================
# Room Database Rules
# ===============================
-keep class com.example.automaticfinances.data.db.** { *; }
-keep @androidx.room.Entity class * {
    <fields>;
    <methods>;
}
-keep @androidx.room.Dao class * {
    <methods>;
}
-keep class * extends androidx.room.RoomDatabase {
    <methods>;
}

# ===============================
# Compose Rules
# ===============================
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class androidx.compose.** {
    <methods>;
}

# ===============================
# Kotlin Coroutines
# ===============================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ===============================
# Data Classes (JSON serialization safe)
# ===============================
-keep class com.example.automaticfinances.data.models.** { *; }
-keep class com.example.automaticfinances.ui.**State { *; }
-keep class com.example.automaticfinances.data.repo.TransactionWithCategory { *; }

# ===============================
# Parser Classes (Critical SMS parsing)
# ===============================
-keep class com.example.automaticfinances.data.parse.** {
    public <methods>;
    public <fields>;
}

# ===============================
# Services & Receivers (Android Components)
# ===============================
-keep class com.example.automaticfinances.system.** {
    public <methods>;
    <init>(...);
}

# ===============================
# ViewModels (Architecture Components)
# ===============================
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
    public <methods>;
}

# ===============================
# Navigation Arguments
# ===============================
-keep class com.example.automaticfinances.navigation.** { *; }

# ===============================
# Apache Commons Codec (Hashing)
# ===============================
-keep class org.apache.commons.codec.** { *; }
-dontwarn org.apache.commons.codec.**

# ===============================
# kotlinx.serialization (Gemini voice NLP layer)
# Aggressive R8 (-repackageclasses, 5 passes) can strip plugin-generated serializers, which
# would crash the voice feature at runtime in the auto-deployed release build. Keep them.
# ===============================
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Belt-and-suspenders: keep the generated serializers for our own wire DTOs.
-keep,includedescriptorclasses class com.example.automaticfinances.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.example.automaticfinances.data.remote.dto.** { *; }

# ===============================
# OkHttp / Okio (Gemini HTTP transport)
# ===============================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ===============================
# Generic Android Rules
# ===============================
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends androidx.lifecycle.ViewModelProvider.NewInstanceFactory

# ===============================
# Reflection Safety
# ===============================
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ===============================
# R8 Optimization Settings
# ===============================
# Allow R8 to optimize aggressively but safely
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# ===============================
# Security: Obfuscate Package Names
# ===============================
-repackageclasses 'a'
-allowaccessmodification
-printmapping build/outputs/mapping/release/mapping.txt

# ===============================
# Remove Logging in Release
# ===============================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ===============================
# Keep Application class
# ===============================
-keep class com.example.automaticfinances.App {
    public <methods>;
    <init>(...);
}