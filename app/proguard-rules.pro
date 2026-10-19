# ProGuard and R8 rules for aMACEzing Sales

# Keep data models used by Room, Moshi, and Firestore
-keepclassmembers class com.amacezing.sales.model.** { *; }
-keep class com.amacezing.sales.model.** { *; }
-keepclassmembers class com.amacezing.sales.data.firebase.** { *; }
-keep class com.amacezing.sales.data.firebase.** { *; }

# Moshi rules
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn javax.annotation.**

# Retrofit rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coroutines rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
