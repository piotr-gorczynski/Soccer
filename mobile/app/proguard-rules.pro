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
# The comprehensive rule below covers all Firebase Auth classes including internal ones
-keep class com.google.firebase.auth.** { *; }

# Firebase Messaging - Keep all classes to prevent broadcast delivery failures.
# Although the Firebase SDK ships its own consumer rules, an explicit rule here
# ensures internal receiver/service classes are never removed by R8 in release builds.
-keep class com.google.firebase.messaging.** { *; }

# Google Play Services Auth - Keep SignInHubActivity and related classes
# to prevent crashes during Google Sign-In flow
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# Facebook SDK - Keep all classes to ensure proper OAuth flow
-keep class com.facebook.** { *; }

# OkHttp (used for Facebook photo fetching)
# Only keep public API to minimize APK size impact
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { public *; }
-keep interface okhttp3.** { *; }

# Keep EnclosingMethod and InnerClasses attributes for better stack traces and
# to support any third-party libraries that rely on reflection-based introspection.
# Note: the app code no longer uses Class.getEnclosingMethod() directly; all log
# helper calls now use hardcoded method name strings to avoid NPE on release builds.
-keepattributes EnclosingMethod,InnerClasses
