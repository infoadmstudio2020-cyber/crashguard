package com.roni.crashguard.demo.app;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.roni.library.crashguard.Breadcrumb;
import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashGuardConfig;
import com.roni.library.crashguard.CrashReport;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * ডেমো অ্যাপের Application ক্লাস।
 *
 * <p>CrashGuard এখানেই ইনস্টল করা হয়, Activity-র আগে, যাতে অ্যাপ চালু হওয়ার
 * প্রথম মুহূর্তের ক্র্যাশও ধরা পড়ে।</p>
 */
public class DemoApplication extends Application {

    private static final String TAG = "CrashGuardDemo";

    @Override
    public void onCreate() {
        super.onCreate();

        // ক্র্যাশ রিপোর্টিং Activity আলাদা প্রসেসে (:crash_guard_process) চলে।
        // সেই প্রসেসে আবার ইনস্টল করার দরকার নেই।
        if (!isMainProcess()) {
            return;
        }

        CrashGuardConfig config = CrashGuardConfig.newBuilder()
            .setEnabled(true)
            .setDebugMode(BuildConfig.DEBUG)
            .setTag("demo-app")
            .setShowCrashActivity(true)
            .setAutoRestart(false)
            .setMaxStoredReports(25)
            .setEnableBreadcrumbs(true)
            .setMaxBreadcrumbs(50)
            .setFilterSensitiveData(true)
            .setShowNotificationOnBackgroundCrash(true)
            .setCrashListener(new com.roni.library.crashguard.CrashListener() {
                @Override
                public void onCrash(Thread thread, Throwable throwable, CrashReport report) {
                    Log.e(TAG, "CrashListener fired for " + report.getExceptionClass()
                        + " on thread " + report.getThreadName());
                }
            })
            .build();

        CrashGuard.install(this, config);

        // ডেমো ডায়াগনস্টিক মেটাডেটা
        CrashGuard.setCustomKey("screen", "app-start");
        CrashGuard.setCustomKey("flavour", "demo");
        CrashGuard.addBreadcrumb("DemoApplication.onCreate() সম্পন্ন", Breadcrumb.Type.SYSTEM, "lifecycle");
    }

    /**
     * অ্যাপটি মূল প্রসেসে চলছে কি না তা যাচাই করে।
     */
    private boolean isMainProcess() {
        String processName = currentProcessName();
        return processName == null || processName.equals(getPackageName());
    }

    private String currentProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return Application.getProcessName();
            } catch (Exception ignored) {
                // নিচের ফলব্যাক ব্যবহার করা হবে
            }
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/self/cmdline"));
            String line = reader.readLine();
            return line != null ? line.trim() : null;
        } catch (Exception e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
