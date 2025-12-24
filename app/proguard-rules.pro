# FRIDAI Android App ProGuard Rules

# Keep Retrofit models
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
