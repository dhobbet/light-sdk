# Rules for the Ktor/Netty wi-fi upload server (UploadServer.kt).
#
# Netty optionally integrates with server-side facilities that do not exist
# on Android: native OpenSSL (tcnative), JDK Flight Recorder, JMX management,
# log4j, and BlockHound. The code paths referencing them are never taken on
# device; R8 just needs to be told the references are expected.
-dontwarn io.netty.internal.tcnative.**
-dontwarn java.lang.management.**
-dontwarn jdk.jfr.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn reactor.blockhound.**
-dontwarn org.eclipse.jetty.alpn.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn org.conscrypt.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn net.jpountz.**
-dontwarn com.ning.compress.**
-dontwarn org.jctools.**

# Netty instantiates its NIO channel classes reflectively
# (ReflectiveChannelFactory), so keep them intact through shrinking.
-keep class io.netty.channel.socket.nio.** { *; }
-keep class io.netty.channel.nio.** { *; }
-keepclassmembers class io.netty.util.** { *; }
