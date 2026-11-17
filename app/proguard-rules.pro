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

# WireGuard GoBackend registers JNI methods by name; keep the backend and crypto classes intact.
-keep class com.wireguard.android.backend.** { *; }
-keep class com.wireguard.crypto.** { *; }
-keep class com.wireguard.config.** { *; }
-dontwarn com.wireguard.**