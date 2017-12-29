# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/yomi/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# picasso
-dontwarn com.squareup.picasso.**
-dontwarn com.squareup.okhttp.**
-keep class com.squareup.** { *; }
-keep interface com.squareup.** { *; }

# apache
-dontwarn org.apache.**
-keep class org.apache.** { *; }
-keep interface org.apache.** { *; }
-keep enum org.apache.** { *; }

# ok
-dontwarn okio.**

# eventbus
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# Fabric configuration
-keepattributes *Annotation*
#-renamesourcefileattribute Proguard
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**