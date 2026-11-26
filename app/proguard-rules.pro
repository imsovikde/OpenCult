# R8 release rules for OpenCult (org.opencult.scale)
-repackageclasses 'o'
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Strip verbose/debug/info logs in release builds while keeping warn/error diagnostics
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# kotlinx-serialization-core and kotlinx-coroutines bundle consumer rules for @Serializable models.
# Keep only runtime annotations/signatures needed by Supabase Ktor/Postgrest and Hilt reified adapters.
-keepattributes *Annotation*,Signature,InnerClasses
