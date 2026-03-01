# ProGuard rules for Ma app
-keepclassmembers class * {
    @androidx.room.* <fields>;
}
-keep class androidx.room.** { *; }
