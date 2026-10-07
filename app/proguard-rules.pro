# ProGuard / R8 rules
-dontwarn javax.lang.model.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.**
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
