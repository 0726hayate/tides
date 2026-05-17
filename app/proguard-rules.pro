# Keep SQLCipher
-keep class net.zetetic.** { *; }
-keep class com.lambdapioneer.argon2kt.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Keep Room
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
