# ─── Folio ProGuard / R8 Rules ────────────────────────────

# Enable R8 full mode optimizations
-allowaccessmodification
-repackageclasses ''
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ─── iText7 ───────────────────────────────────────────────
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# ─── Apache POI ──────────────────────────────────────────
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class org.openxmlformats.** { *; }
-dontwarn org.openxmlformats.**
-keep class schemaorg_apache_xmlbeans.** { *; }
-dontwarn schemaorg_apache_xmlbeans.**
-keep class org.apache.commons.** { *; }
-dontwarn org.apache.commons.**
-keep class com.microsoft.schemas.** { *; }
-dontwarn com.microsoft.schemas.**
-keep class org.apache.logging.** { *; }
-dontwarn org.apache.logging.**

# ─── Room ─────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# ─── Hilt / Dagger ────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.internal.codegen.**
-dontwarn com.google.errorprone.annotations.**

# ─── Google Play Billing ──────────────────────────────────
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }

# ─── AdMob ────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# ─── Lottie ──────────────────────────────────────────────
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ─── Coil ─────────────────────────────────────────────────
-dontwarn coil.**
-keep class coil.** { *; }

# ─── DataStore ────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ─── Kotlin / Coroutines ─────────────────────────────────
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.debug.**
-keep class kotlinx.coroutines.** { *; }

# ─── Compose ─────────────────────────────────────────────
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ─── Keep Folio models used with Room / Serialization ─────
-keep class com.folio.data.local.entity.** { *; }
-keep class com.folio.domain.model.** { *; }

# ─── General ──────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep data classes used with Room
-keep class com.folio.data.local.entity.** { *; }
-keep class com.folio.domain.model.** { *; }
