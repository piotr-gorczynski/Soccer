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

# Keep EnclosingMethod and InnerClasses attributes so that anonymous inner
# classes created via "new Object(){}" retain their getEnclosingMethod()
# reference at runtime.  Without this, R8/ProGuard strips these attributes in
# release builds and Class.getEnclosingMethod() returns null, causing the
# NullPointerException inside MenuActivity.runHousekeeping (and similar log
# helper sites across the app) that wraps the result in Objects.requireNonNull().
-keepattributes EnclosingMethod,InnerClasses
