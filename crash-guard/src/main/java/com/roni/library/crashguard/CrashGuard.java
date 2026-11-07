package com.roni.library.crashguard;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main public entry point for the CrashGuard Android crash defense and telemetry library.
 *
 * <p>Usage:</p>
 * <pre>
 * CrashGuard.install(this, CrashGuardConfig.newBuilder()
 *     .setDebugMode(true)
 *     .setAutoRestart(false)
 *     .setMaxStoredReports(20)
 *     .build());
 * </pre>
 *
 * <p>Install from {@code Application.onCreate()} so that crashes occurring during
 * the very first Activity launch are already covered.</p>
 */
public final class CrashGuard {

    private static final String TAG = "CrashGuard";
    private static final String LIBRARY_VERSION = "1.0.0";

    private static volatile CrashGuard instance;
    private static final Object INIT_LOCK = new Object();

    private final Context context;
    private final CrashGuardConfig config;
    private final CrashReportStorage storage;
    private final BreadcrumbTracker breadcrumbTracker;
    private final Map<String, String> customData = new ConcurrentHashMap<>();
    private final CrashHandler crashHandler;
    private final Thread.UncaughtExceptionHandler previousHandler;

    private CrashGuard(Context context, CrashGuardConfig config) {
        this.context = context.getApplicationContext();
        this.config = config != null ? config : CrashGuardConfig.defaultConfig();
        this.storage = new CrashReportStorage(this.context, this.config.getMaxStoredReports());
        this.breadcrumbTracker = new BreadcrumbTracker(this.config.getMaxBreadcrumbs());

        // Capture the handler that was active before CrashGuard so uninstall()
        // can restore it and so a disabled config can delegate correctly.
        this.previousHandler = Thread.getDefaultUncaughtExceptionHandler();

        this.crashHandler = new CrashHandler(
            this.context,
            this.config,
            this.storage,
            this.breadcrumbTracker,
            this.customData,
            this.previousHandler
        );

        // Register default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this.crashHandler);

        // Seed initial breadcrumbs directly on the tracker: the static helper
        // cannot be used here because `instance` is still null during construction.
        this.breadcrumbTracker.add(
            "CrashGuard initialized (v" + LIBRARY_VERSION + ")",
            Breadcrumb.Type.SYSTEM,
            "init"
        );
    }

    /**
     * Initializes CrashGuard with default configuration settings.
     *
     * @param context Application or Activity context.
     * @return Initialized CrashGuard instance.
     */
    public static CrashGuard install(Context context) {
        return install(context, CrashGuardConfig.defaultConfig());
    }

    /**
     * Initializes CrashGuard with custom configuration options.
     *
     * <p>Calling this method more than once is safe: the first configuration wins
     * and subsequent calls return the existing instance.</p>
     *
     * @param context Application or Activity context.
     * @param config Custom CrashGuardConfig builder options.
     * @return Initialized CrashGuard instance.
     */
    public static CrashGuard install(Context context, CrashGuardConfig config) {
        if (instance == null) {
            synchronized (INIT_LOCK) {
                if (instance == null) {
                    if (context == null) {
                        throw new IllegalArgumentException("Context cannot be null when installing CrashGuard.");
                    }
                    instance = new CrashGuard(context, config);
                    if (instance.config.isDebugMode()) {
                        Log.i(TAG, "CrashGuard initialized successfully.");
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Returns the singleton instance if installed, or null.
     */
    public static CrashGuard getInstance() {
        return instance;
    }

    /**
     * Checks if CrashGuard has been installed in the current process.
     */
    public static boolean isInstalled() {
        return instance != null;
    }

    /**
     * Alias of {@link #isInstalled()} kept for API symmetry with older docs.
     */
    public static boolean isInitialized() {
        return isInstalled();
    }

    /**
     * Uninstalls CrashGuard and restores the previously registered uncaught
     * exception handler. Stored reports are kept on disk.
     */
    public static void uninstall() {
        synchronized (INIT_LOCK) {
            if (instance == null) return;
            Thread.setDefaultUncaughtExceptionHandler(instance.previousHandler);
            instance.breadcrumbTracker.clear();
            instance.customData.clear();
            instance = null;
        }
    }

    /**
     * Alias of {@link #uninstall()}. After calling this, {@link #getInstance()}
     * returns {@code null} and a later {@link #install(Context)} creates a fresh
     * instance.
     */
    public static void release() {
        uninstall();
    }

    /**
     * Appends a user or diagnostic breadcrumb to the sliding window ring buffer.
     */
    public static void addBreadcrumb(String message, Breadcrumb.Type type, String category) {
        CrashGuard local = instance;
        if (local != null && local.config.isEnableBreadcrumbs()) {
            local.breadcrumbTracker.add(message, type, category);
        }
    }

    /**
     * Convenience method to append a user action breadcrumb.
     */
    public static void addBreadcrumb(String message) {
        addBreadcrumb(message, Breadcrumb.Type.USER_ACTION, "user");
    }

    /**
     * Sets a key-value diagnostic metadata tag to be included in subsequent crash reports.
     */
    public static void setCustomKey(String key, String value) {
        CrashGuard local = instance;
        if (local != null && key != null) {
            if (value == null) {
                local.customData.remove(key);
            } else {
                local.customData.put(key, value);
            }
        }
    }

    /**
     * Captures and stores a non-fatal, caught exception without terminating the application.
     */
    public static void logHandledException(Throwable throwable) {
        CrashGuard local = instance;
        if (local == null || throwable == null) return;
        if (!local.config.isEnabled()) return;

        CrashReport report = new CrashReport();
        report.setReportType(CrashReport.TYPE_HANDLED);
        report.setTag(local.config.getTag());
        report.setThreadName(Thread.currentThread().getName());
        report.setThreadId(Thread.currentThread().getId());
        report.setMainThread(Looper.getMainLooper() != null
            && Looper.getMainLooper().getThread() == Thread.currentThread());
        report.setExceptionClass(throwable.getClass().getName());

        String message = throwable.getMessage() != null ? throwable.getMessage() : "";
        if (local.config.isFilterSensitiveData()) {
            message = local.sanitize(message);
        }
        report.setExceptionMessage("[HANDLED] " + message);

        String trace = CrashReport.getStackTraceString(throwable);
        if (local.config.isFilterSensitiveData()) {
            trace = local.sanitize(trace);
        }
        report.setStackTrace(trace);

        if (throwable.getCause() != null) {
            report.setCause(throwable.getCause().toString());
        }

        // Full environment snapshot, identical to the uncaught path.
        report.setAppPackage(local.context.getPackageName());
        report.setAppVersionName(DeviceUtils.getAppVersionName(local.context));
        report.setAppVersionCode(DeviceUtils.getAppVersionCode(local.context));
        report.setAndroidVersion(Build.VERSION.RELEASE);
        report.setSdkInt(Build.VERSION.SDK_INT);
        report.setDeviceManufacturer(Build.MANUFACTURER);
        report.setDeviceModel(Build.MODEL);
        report.setDeviceProduct(Build.PRODUCT);
        report.setRooted(DeviceUtils.isDeviceRooted());
        report.setFreeRamMb(DeviceUtils.getFreeRamMb(local.context));
        report.setTotalRamMb(DeviceUtils.getTotalRamMb(local.context));
        report.setFreeStorageMb(DeviceUtils.getAvailableInternalStorageMb());
        report.setTotalStorageMb(DeviceUtils.getTotalInternalStorageMb());

        report.setBreadcrumbs(local.breadcrumbTracker.getAll());
        report.setCustomData(new HashMap<>(local.customData));

        local.storage.saveReport(report);

        if (local.config.isEnableBreadcrumbs()) {
            local.breadcrumbTracker.add(
                "Logged handled exception: " + throwable.getClass().getSimpleName(),
                Breadcrumb.Type.SYSTEM,
                "error"
            );
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        String sanitized = input.replaceAll("(?i)(password|passwd|pwd|secret)\\s*=\\s*[^&,\\s]+", "$1=***REDACTED***");
        return sanitized.replaceAll("(?i)Bearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*", "Bearer ***REDACTED***");
    }

    /**
     * Retrieves all saved crash reports persisted on disk.
     */
    public static List<CrashReport> getStoredReports() {
        CrashGuard local = instance;
        if (local != null) {
            return local.storage.getAllReports();
        }
        return Collections.emptyList();
    }

    /**
     * Returns the most recent stored report, or {@code null} when none exists.
     */
    public static CrashReport getLatestReport() {
        CrashGuard local = instance;
        return local != null ? local.storage.getLatestReport() : null;
    }

    /**
     * Number of reports currently persisted on disk.
     */
    public static int getStoredReportCount() {
        CrashGuard local = instance;
        return local != null ? local.storage.getReportCount() : 0;
    }

    /**
     * Deletes a specific stored crash report.
     */
    public static boolean deleteReport(String reportId) {
        CrashGuard local = instance;
        if (local != null) {
            return local.storage.deleteReport(reportId);
        }
        return false;
    }

    /**
     * Clears all crash reports from disk storage.
     */
    public static void clearReports() {
        CrashGuard local = instance;
        if (local != null) {
            local.storage.clearAllReports();
        }
    }

    public CrashGuardConfig getConfig() {
        return config;
    }

    public CrashReportStorage getStorage() {
        return storage;
    }

    public BreadcrumbTracker getBreadcrumbTracker() {
        return breadcrumbTracker;
    }

    public CrashHandler getCrashHandler() {
        return crashHandler;
    }

    public Context getContext() {
        return context;
    }

    public String getLibraryVersion() {
        return LIBRARY_VERSION;
    }
}
