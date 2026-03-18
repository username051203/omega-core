# Omega Protocol ProGuard rules

# Keep Room entities
-keep class com.omega.protocol.db.entity.** { *; }
-keep class com.omega.protocol.model.** { *; }
-keepclassmembers class com.omega.protocol.model.** { *; }

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Navigation
-keepnames class androidx.navigation.fragment.NavHostFragment

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# ColorPickerView
-keep class com.skydoves.colorpickerview.** { *; }

# General
-dontwarn okhttp3.**
-dontwarn okio.**
