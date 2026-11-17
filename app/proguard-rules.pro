# youtubedl-android maps yt-dlp JSON into POJOs with Jackson reflection; R8 must not strip them.
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.youtubedl_common.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.**
-dontwarn javax.annotation.**