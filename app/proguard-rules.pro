# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Navigation 3: @Serializable NavKey route classes (nav/Routes.kt).
# rememberNavBackStack() restores the back stack with NavKeySerializer, which looks up
# the concrete class via reflection (Class.forName) and its KSerializer. Keep the class
# names and members unobfuscated so reflection restore works under R8/minify.
-keep class site.xiaozk.dailyfitness.nav.** { *; }

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