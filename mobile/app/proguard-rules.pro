# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Firebase Auth - Keep GenericIdpActivity and related classes to prevent crashes
# during OAuth sign-in flow (Google, Microsoft, Facebook)
-keep class com.google.firebase.auth.internal.** { *; }
-keep class com.google.firebase.auth.api.** { *; }
-keepclassmembers class com.google.firebase.auth.internal.GenericIdpActivity {
    *;
}

# Keep Firebase Auth public API
-keep class com.google.firebase.auth.** { *; }
-keepclassmembers class com.google.firebase.auth.** { *; }

# Facebook SDK
-keep class com.facebook.** { *; }
-keepclassmembers class com.facebook.** { *; }

# OkHttp (used for Facebook photo fetching)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
