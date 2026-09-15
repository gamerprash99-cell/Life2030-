# LifeOS proguard rules

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# Keep data/domain models used for JSON backup/export & AI payloads
-keep class com.lifeos.app.data.db.entities.** { *; }
-keep class com.lifeos.app.domain.model.** { *; }
-keep class com.lifeos.app.core.ai.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
