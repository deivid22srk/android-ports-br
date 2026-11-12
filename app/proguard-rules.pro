# Android Ports BR — regras mínimas de R8 (full mode)
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations
-keep,includedescriptorclasses class br.androidports.**$$serializer { *; }
-keepclassmembers class br.androidports.** {
    *** Companion;
}
-keepclasseswithmembers class br.androidports.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit (as regras do próprio artefato cobrem o básico)
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coil
-dontwarn coil3.**

# Modelos serializados (nomes estáveis ajudam no cache e nos logs)
-keep class br.androidports.data.api.** { *; }
