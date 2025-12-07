##############################
# Основные настройки
##############################
# Не показывать предупреждения для сторонних библиотек
-dontwarn org.intellij.lang.annotations.**
-dontwarn javax.annotation.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn com.google.gson.**
-dontwarn com.squareup.okhttp3.**
-dontwarn com.squareup.picasso.**
-dontwarn com.squareup.retrofit2.**
-dontwarn com.vk.**
-dontwarn com.yandex.**

# Сохраняем имена классов и методов с аннотациями @Keep, @SerializedName и т.д.
-keepattributes *Annotation*, Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

##############################
# VK ID SDK
##############################
-keep class com.vk.** { *; }
-keepclassmembers class com.vk.** { *; }

##############################
# Firebase
##############################
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

##############################
# Yandex Maps
##############################
-keep class com.yandex.mapkit.** { *; }
-dontwarn com.yandex.mapkit.**

##############################
# Retrofit + OkHttp + Gson
##############################
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }

# Сохраняем модели данных (если используешь Gson/Retrofit)
# Можно указать свой пакет с моделями:
-keep class ru.dontour.model.** { *; }

##############################
# Picasso
##############################
-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**

##############################
# AndroidX / Compose
##############################
-keep class androidx.** { *; }
-keep class kotlinx.** { *; }
-dontwarn androidx.**
-dontwarn kotlinx.**

##############################
# Тесты и отладка
##############################
# (не обязательно, но удобно)
-keep class androidx.test.** { *; }
-keep class junit.** { *; }

##############################
# Общее
##############################
# Не обфусцировать ресурсы R
-keepclassmembers class **.R$* {
    public static <fields>;
}
