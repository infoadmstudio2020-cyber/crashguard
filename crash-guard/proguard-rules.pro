# ---------------------------------------------------------------------------
# R8 rules applied while minifying the CrashGuard library itself.
# Consumer-facing rules live in consumer-rules.pro.
# ---------------------------------------------------------------------------

# Keep the public API surface stable across releases.
-keep public class com.roni.library.crashguard.CrashGuard { public *; }
-keep public class com.roni.library.crashguard.CrashGuardConfig { public *; }
-keep public class com.roni.library.crashguard.CrashGuardConfig$Builder { public *; }
-keep public interface com.roni.library.crashguard.CrashListener { *; }

# Models are serialised to JSON by field name.
-keep class com.roni.library.crashguard.CrashReport { *; }
-keep class com.roni.library.crashguard.Breadcrumb { *; }

# Manifest-declared component.
-keep class com.roni.library.crashguard.DefaultCrashActivity { *; }

# Do not warn about optional AndroidX annotations.
-dontwarn androidx.annotation.**
