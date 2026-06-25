# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.ssafy.e106.**$$serializer { *; }
-keepclassmembers class com.ssafy.e106.** {
    *** Companion;
}

# Firebase
-keep class com.google.firebase.** { *; }
