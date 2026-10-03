# LifeOS proguard rules

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# Keep data/domain models used for JSON backup/export
-keep class com.lifeos.app.data.db.entities.** { *; }
-keep class com.lifeos.app.domain.model.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SQLCipher (net.zetetic)
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**
