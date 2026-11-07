# ---------------------------------------------------------------------------
# CrashGuard consumer ProGuard / R8 rules
# These rules are shipped inside the AAR and applied automatically to any app
# that depends on the library.
# ---------------------------------------------------------------------------

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,Exceptions

# Public entry point and configuration
-keep class com.roni.library.crashguard.CrashGuard { public *; }
-keep class com.roni.library.crashguard.CrashGuardConfig { *; }
-keep class com.roni.library.crashguard.CrashGuardConfig$Builder { *; }
-keep interface com.roni.library.crashguard.CrashListener { *; }

# Report model - serialised to JSON and (legacy) to Intent extras
-keep class com.roni.library.crashguard.CrashReport { *; }
-keep class com.roni.library.crashguard.Breadcrumb { *; }
-keep class com.roni.library.crashguard.Breadcrumb$Type { *; }

# Components referenced from the manifest / reflection
-keep class com.roni.library.crashguard.DefaultCrashActivity { *; }
-keep class com.roni.library.crashguard.CrashHandler { *; }
-keep class com.roni.library.crashguard.CrashReportStorage { *; }
-keep class com.roni.library.crashguard.BreadcrumbTracker { *; }
-keep class com.roni.library.crashguard.CrashNotificationManager { *; }
-keep class com.roni.library.crashguard.DeviceUtils { *; }

# Java serialisation contract (CrashReport implements Serializable)
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enum valueOf/values used by Breadcrumb.Type
-keepclassmembers enum com.roni.library.crashguard.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
