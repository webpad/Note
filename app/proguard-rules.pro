# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html

# Optimizations: If you don't want to optimize, use the
# proguard-android.txt configuration file instead of this one, which
# turns off the optimization flags.  Adding optimization introduces
# certain risks, since for example not all optimizations performed by
# ProGuard works on all versions of Dalvik.  The following flags turn
# off various optimizations known to have issues, but the list may not
# be complete or up to date. (The "arithmetic" optimization can be
# used if you are only targeting Android 2.0 or later.)  Make sure you
# test thoroughly if you go this route.

-dontwarn android.**
-keep class android.** { *;}
-dontwarn sun.misc.**
-keep class com.google.gson.** { *;}
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.** { *;}
-dontwarn org.libharu.**
-keep class org.libharu.** { *;}
# -dontwarn org.apache.http.entity.mime.**
# -keep class org.apache.http.entity.mime.** { *;}
-dontwarn com.dropbox.client2.**
-keep class com.dropbox.client2.** { *;}
# -dontwarn org.json.simple.**
# -keep class org.json.simple.** { *;}
-dontwarn com.dropbox.**
-keep class com.dropbox.** { *;}

-dontwarn name.vbraun.view.write.**
-keep class name.vbraun.view.write.** { *;}

-dontwarn ntx.draw.**
-keep class ntx.draw.** { *;}

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# The remainder of this file is identical to the non-optimized version
# of the Proguard configuration file (except that the other file has
# flags to turn off optimization).

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-ignorewarnings

-keepattributes *Annotation*
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService



# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-keepattributes Signature
# -keep class ntx.note.bookshelf.RecentlyBookData
# -keep class ntx.note.CallbackEvent
# -keep class ntx.note.PageBackgroundChangeEvent
# -keep class ntx.note.ToolboxConfigurationn
# -keepclassmembers class ntx.note.bookshelf.RecentlyBookData { *;}
# -keepclassmembers class ntx.note.CallbackEvent { *;}
# -keepclassmembers class ntx.note.PageBackgroundChangeEvent { *;}
# -keepclassmembers class ntx.note.ToolboxConfigurationn { *;}

-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
