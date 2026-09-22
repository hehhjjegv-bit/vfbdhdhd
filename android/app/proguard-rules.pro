# Quick File Studio - R8 protection

# Keep the Android JavaScript bridge class and its callable methods.
-keep class com.quickfilestudio.app.bridge.AndroidBridge { *; }

# Keep JavaScript interface methods.
-keepclassmembers class com.quickfilestudio.app.bridge.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Android components.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Keep native/reflective entry points.
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}

# Keep JSON-related model/bridge compatibility.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Remove debug logging from release builds where possible.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Do not emit source file/line information into the optimized release.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Strong obfuscation for application implementation classes.
# Android entry points and AndroidBridge are protected above.
-keep,allowoptimization,allowobfuscation class com.quickfilestudio.app.storage.** { *; }
-keep,allowoptimization,allowobfuscation class com.quickfilestudio.app.update.** { *; }

# Allow R8 to rename internal classes/methods/fields.
-allowaccessmodification

# Keep only names required by serialization/reflection annotations.
-keepnames @android.webkit.JavascriptInterface class * { *; }

# Obfuscate remaining application code aggressively.
-overloadaggressively

# Remove unused code and metadata where safe.
-dontusemixedcaseclassnames
-dontpreverify

# Keep enum values required by Java runtime.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
