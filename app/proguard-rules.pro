# youtubedl-android maps yt-dlp JSON into POJOs with Jackson reflection; R8 must not strip them.
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.youtubedl_common.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.**
-dontwarn javax.annotation.**

# NewPipeExtractor resolves YouTube streams at runtime; its Rhino JS engine is reflective.
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.schabi.newpipe.extractor.**

# OkHttp references optional TLS providers that are not bundled.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# bt (BitTorrent) wires its internals through Guice reflection; R8 must keep the graph intact.
-keep class bt.** { *; }
-keep class lbms.plugins.mldht.** { *; }
-keep class com.google.inject.** { *; }
-keep class javax.inject.** { *; }
-keep class org.yaml.snakeyaml.** { *; }
-keepclassmembers class * {
    @com.google.inject.Inject <fields>;
    @com.google.inject.Inject <init>(...);
    @com.google.inject.Provides <methods>;
}
-dontwarn bt.**
-dontwarn com.google.inject.**
-dontwarn org.slf4j.**
-dontwarn java.beans.**