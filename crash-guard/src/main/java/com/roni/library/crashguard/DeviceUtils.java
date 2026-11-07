package com.roni.library.crashguard;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.util.Locale;

/**
 * Diagnostic utilities for inspecting device state, hardware, and OS environment.
 *
 * <p>Every method is defensive: a failure returns a neutral value instead of
 * throwing, because these helpers run inside the crash handler.</p>
 */
public final class DeviceUtils {

    private DeviceUtils() {}

    public static String getAppVersionName(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            return info.versionName != null ? info.versionName : "1.0.1";
        } catch (Exception e) {
            return "unknown";
        }
    }

    @SuppressWarnings("deprecation")
    public static long getAppVersionCode(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return info.getLongVersionCode();
            } else {
                return info.versionCode;
            }
        } catch (Exception e) {
            return 1L;
        }
    }

    public static long getTotalRamMb(Context context) {
        try {
            ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            if (actManager != null) {
                actManager.getMemoryInfo(memInfo);
                return memInfo.totalMem / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static long getFreeRamMb(Context context) {
        try {
            ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            if (actManager != null) {
                actManager.getMemoryInfo(memInfo);
                return memInfo.availMem / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static long getAvailableInternalStorageMb() {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long bytesAvailable = stat.getAvailableBytes();
            return bytesAvailable / (1024 * 1024);
        } catch (Exception ignored) {}
        return 0;
    }

    public static long getTotalInternalStorageMb() {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long totalBytes = stat.getTotalBytes();
            return totalBytes / (1024 * 1024);
        } catch (Exception ignored) {}
        return 0;
    }

    /** BCP-47 language tag of the current device locale, e.g. {@code bn-BD}. */
    public static String getLocaleTag() {
        try {
            Locale locale = Locale.getDefault();
            return locale.toLanguageTag();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static boolean isDeviceRooted() {
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        };
        for (String path : paths) {
            try {
                if (new File(path).exists()) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
