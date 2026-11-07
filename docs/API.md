# CrashGuard API Reference

Package: `com.roni.library.crashguard`
Artifact: `com.roni.library:crash-guard:1.0.1`

---

## `CrashGuard`

Singleton facade. All static methods are null-safe: calling them before `install()`
is a no-op rather than a crash.

### Installation

```java
public static CrashGuard install(Context context)
public static CrashGuard install(Context context, CrashGuardConfig config)
```

Installs the uncaught exception handler. **Idempotent** — a second call returns the
existing instance and does not re-register the handler.

- `context` — any `Context`; the application context is used internally.
- Throws `IllegalArgumentException` if `context` is `null`.
- If `config.isEnabled()` is `false`, the call returns `null` and nothing is installed.

```java
public static void uninstall()
public static void release()          // alias of uninstall()
```

Restores the handler that was active before `install()` and clears the singleton.

### State

```java
public static boolean isInstalled()
public static boolean isInitialized()          // alias of isInstalled()
public static CrashGuard getInstance()         // null when not installed
public String getLibraryVersion()              // "1.0.1"
public CrashGuardConfig getConfig()
public CrashReportStorage getStorage()
public BreadcrumbTracker getBreadcrumbTracker()
```

### Breadcrumbs

```java
public static void addBreadcrumb(String message)
public static void addBreadcrumb(String message, Breadcrumb.Type type, String category)
```

`Breadcrumb.Type`: `USER_ACTION`, `NAVIGATION`, `NETWORK`, `SYSTEM`, `CUSTOM`.

Breadcrumbs live in a synchronised ring buffer capped at
`CrashGuardConfig.getMaxBreadcrumbs()`. The oldest entry is evicted first.

### Custom metadata

```java
public static void setCustomKey(String key, String value)   // value == null removes the key
```

Keys are attached to every report created afterwards. Values are masked when
`isFilterSensitiveData()` is enabled and the key looks sensitive.

### Non-fatal reports

```java
public static void logHandledException(Throwable throwable)
```

Persists a report with `reportType = TYPE_HANDLED` and returns immediately.
`null` is ignored. The app keeps running.

### Reading reports

```java
public static List<CrashReport> getStoredReports()   // newest first, never null
public static CrashReport getLatestReport()          // null when empty
public static int getStoredReportCount()
public static boolean deleteReport(String reportId)
public static void clearReports()
```

---

## `CrashGuardConfig`

Immutable. Build with `CrashGuardConfig.newBuilder()`.

| Builder method | Default | Notes |
|---|---|---|
| `setEnabled(boolean)` | `true` | `false` makes `install()` a no-op |
| `setDebugMode(boolean)` | `true` | Logs to Logcat when enabled |
| `setDebugEnabled(boolean)` | — | Alias of `setDebugMode` |
| `setTag(String)` | `"CrashGuard"` | Blank/null falls back to the default |
| `setShowCrashActivity(boolean)` | `true` | Show the recovery screen |
| `setCustomCrashActivityClass(Class<?>)` | `null` | Must extend `Activity` |
| `setRestartActivityClass(Class<?>)` | `null` | `null` → launcher intent |
| `setAutoRestart(boolean)` | `false` | Restart after the crash screen |
| `setRestartDelayMs(long)` | `1200` | Clamped to ≥ `200` |
| `setMaxStoredReports(int)` | `25` | Clamped to ≥ `1` |
| `setEnableBreadcrumbs(boolean)` | `true` | |
| `setMaxBreadcrumbs(int)` | `50` | Clamped to ≥ `5` |
| `setFilterSensitiveData(boolean)` | `true` | Masks passwords/tokens |
| `setShowNotificationOnBackgroundCrash(boolean)` | `true` | |
| `setCrashListener(CrashListener)` | `null` | Invoked on the crashing thread |

### `CrashListener`

```java
public interface CrashListener {
    void onCrash(Thread thread, Throwable throwable, CrashReport report);
}
```

Called **before** the process is killed. Keep it fast and non-throwing; do not block
on network I/O here — persist or hand off instead.

---

## `CrashReport`

`Serializable` value object (`serialVersionUID = 2L`).

### Identity

| Method | Description |
|---|---|
| `getReportId()` | UUID assigned at creation |
| `getTimestamp()` | `System.currentTimeMillis()` |
| `getFormattedDate()` | `yyyy-MM-dd HH:mm:ss` |
| `getReportType()` | `TYPE_UNCAUGHT` or `TYPE_HANDLED` |
| `isHandledReport()` | `reportType == TYPE_HANDLED` |
| `getTag()` | Config tag at capture time |

### Exception

`getExceptionClass()`, `getExceptionMessage()`, `getStackTrace()`, `getCause()`

### Thread

`getThreadName()`, `getThreadId()`, `isMainThread()`

### Application

`getAppPackage()`, `getAppVersionName()`, `getAppVersionCode()`

### Device

`getAndroidVersion()`, `getSdkInt()`, `getDeviceManufacturer()`, `getDeviceModel()`,
`getDeviceProduct()`, `isRooted()`

### Resources

`getFreeRamMb()`, `getTotalRamMb()`, `getFreeStorageMb()`, `getTotalStorageMb()`

### Context

`getBreadcrumbs()` → `List<Breadcrumb>` (defensive copy)
`getCustomData()` → `Map<String, String>` (defensive copy)

### Serialisation

```java
public JSONObject toJson()
public String toJsonString()
public static CrashReport fromJson(JSONObject json)   // null-safe
public String toFormattedString()                     // human-readable, for sharing
public String getSummary()                            // one line, ≤ 120 chars
```

---

## `Breadcrumb`

```java
public Breadcrumb(String message, Breadcrumb.Type type, String category)
public Breadcrumb(String message)                     // type = USER_ACTION
```

| Method | Description |
|---|---|
| `getMessage()` | Breadcrumb text |
| `getType()` | `Breadcrumb.Type` |
| `getCategory()` | Optional grouping label |
| `getTimestamp()` | `System.currentTimeMillis()` |
| `getFormattedTime()` | `HH:mm:ss.SSS` |
| `toJson()` / `toJsonString()` | Serialisation |
| `fromJson(JSONObject)` | Null-safe factory |

---

## `CrashReportStorage`

```java
public CrashReportStorage(Context context, int maxReports)

public boolean saveReport(CrashReport report)     // atomic write
public CrashReport getReport(String reportId)
public List<CrashReport> getAllReports()          // newest first
public CrashReport getLatestReport()
public int getReportCount()
public boolean deleteReport(String reportId)
public void clearAllReports()
public String exportAllAsText()
public File getStorageDir()
```

Reports are stored as `<reportId>.json` in
`context.getFilesDir()/crash_reports/`. Writes go to `<reportId>.json.tmp`, are
`fsync`ed, then renamed. When `maxReports` is exceeded the oldest files are deleted.

---

## `BreadcrumbTracker`

```java
public BreadcrumbTracker(int maxSize)             // clamped to ≥ 5
public void add(Breadcrumb breadcrumb)            // null ignored
public void add(String message)
public void add(String message, Breadcrumb.Type type, String category)
public List<Breadcrumb> getAll()                  // defensive copy
public int size()
public void clear()
public String toJsonArray()
```

---

## `DefaultCrashActivity`

Runs in the `:crash_guard_process` process.

| Constant | Value | Description |
|---|---|---|
| `EXTRA_REPORT_ID` | `"extra_crashguard_report_id"` | Primary payload |
| `EXTRA_CRASH_REPORT` | `"extra_crashguard_report"` | Legacy serialisable payload |

Resolution order: `EXTRA_REPORT_ID` → `EXTRA_CRASH_REPORT` → latest report on disk.

Override with `CrashGuardConfig.setCustomCrashActivityClass(...)`.

---

## `CrashNotificationManager`

```java
public static void showCrashNotification(Context context, CrashReport report)
public static void cancel(Context context)
```

The notification channel (`crashguard_diagnostics_channel`) is created on demand and is
an implementation detail — it is not part of the public API.

Requires `POST_NOTIFICATIONS` on API 33+. The library declares the permission; the
host app must request it at runtime.

---

## `DeviceUtils`

```java
public static String getDeviceInfo()
public static String getAppVersionName(Context context)
public static int getAppVersionCode(Context context)
public static boolean isRooted()
public static long getFreeRamMb()
public static long getTotalRamMb()
public static long getFreeStorageMb()
public static long getTotalStorageMb()
```
