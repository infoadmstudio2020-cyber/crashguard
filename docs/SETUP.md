# CrashGuard Setup Guide

## Requirements

| Requirement | Version |
|---|---|
| Android Gradle Plugin | 8.1+ (verified on 8.3.2) |
| Gradle | 8.0+ (verified on 8.7) |
| JDK | 17 |
| `compileSdk` | 34 |
| `minSdk` | 21 |
| AndroidX | required |

---

## 1. Add the dependency

### JitPack

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```gradle
// app/build.gradle
dependencies {
    implementation 'com.github.infoadmstudio2020-cyber.crash-guard:crash-guard:1.0.1'
}
```

### Maven Local

```bash
./gradlew :crash-guard:publishReleasePublicationToMavenLocal
```

```gradle
repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    implementation 'com.roni.library:crash-guard:1.0.1'
}
```

### GitHub Packages

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/infoadmstudio2020-cyber/crash-guard")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### Source module

```gradle
// settings.gradle
include ':crash-guard'
project(':crash-guard').projectDir = new File(rootDir, 'crash-guard')

// app/build.gradle
dependencies {
    implementation project(':crash-guard')
}
```

---

## 2. Enable AndroidX

`gradle.properties`:

```properties
android.useAndroidX=true
android.nonTransitiveRClass=true
```

Jetifier is **not** required — every dependency is already AndroidX.

---

## 3. Install in `Application`

```java
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        CrashGuardConfig config = CrashGuardConfig.newBuilder()
            .setEnabled(true)
            .setDebugMode(BuildConfig.DEBUG)
            .setTag("my-app")
            .setShowCrashActivity(true)
            .setAutoRestart(false)
            .setMaxStoredReports(25)
            .setEnableBreadcrumbs(true)
            .setMaxBreadcrumbs(50)
            .setFilterSensitiveData(true)
            .setShowNotificationOnBackgroundCrash(true)
            .build();

        CrashGuard.install(this, config);
    }
}
```

Register it in `AndroidManifest.xml`:

```xml
<application android:name=".MyApp" ... >
```

> **Why `Application` and not an `Activity`?** Crashes during startup happen before any
> activity exists. Installing in `Application.onCreate()` covers the whole process.

---

## 4. Multi-process apps

`DefaultCrashActivity` runs in `:crash_guard_process`, so your `Application` is created
**twice**. Guard expensive initialisation:

```java
@Override
public void onCreate() {
    super.onCreate();

    if (isMainProcess()) {
        CrashGuard.install(this, config);
        // ... other heavy setup
    }
}

private boolean isMainProcess() {
    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
    if (processes == null) return true;
    for (ActivityManager.RunningAppProcessInfo info : processes) {
        if (info.pid == Process.myPid()) {
            return getPackageName().equals(info.processName);
        }
    }
    return true;
}
```

---

## 5. Notifications on Android 13+

The library declares `POST_NOTIFICATIONS` in its manifest, but the runtime request is
the host app's responsibility:

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
    }
}
```

Without it, background-thread crashes are still recorded — only the notification is
skipped.

---

## 6. Custom crash screen

```java
CrashGuardConfig.newBuilder()
    .setCustomCrashActivityClass(MyCrashActivity.class)
    .build();
```

```java
public class MyCrashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String reportId = getIntent().getStringExtra(DefaultCrashActivity.EXTRA_REPORT_ID);
        CrashReport report = reportId == null ? null
            : CrashGuard.getInstance().getStorage().getReport(reportId);

        // render report ...
    }
}
```

Declare it in your manifest with the same process:

```xml
<activity
    android:name=".MyCrashActivity"
    android:process=":crash_guard_process"
    android:exported="false" />
```

---

## 7. ProGuard / R8

The library ships `consumer-rules.pro`, applied automatically. It keeps:

- `CrashHandler` (referenced by the framework)
- `CrashReportStorage` (reflection-free, but kept for stable stack traces)
- `BreadcrumbTracker`, `CrashNotificationManager`, `DeviceUtils`
- `CrashReport` / `Breadcrumb` `Serializable` members

No manual rules are needed.

---

## 8. Verify the integration

```java
Log.d("CrashGuard", "installed=" + CrashGuard.isInstalled()
    + " version=" + CrashGuard.getInstance().getLibraryVersion()
    + " reports=" + CrashGuard.getStoredReportCount());
```

Then trigger a crash and confirm the recovery screen appears. See
[`CRASH_TESTING_GUIDE.md`](CRASH_TESTING_GUIDE.md).

---

## 9. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `ClassNotFoundException: com.roni.library.crashguard.DefaultCrashActivity` | Activity not merged into the manifest | Ensure `minifyEnabled` rules are intact and the AAR is on the runtime classpath |
| Crash screen never appears | `setShowCrashActivity(false)` or a custom activity that is not declared | Check the config and the manifest |
| No notification on Android 13+ | `POST_NOTIFICATIONS` not granted | Request it at runtime |
| `TransactionTooLargeException` | Custom crash activity reading the legacy `EXTRA_CRASH_REPORT` | Use `EXTRA_REPORT_ID` and load from storage |
| Reports not persisted | Storage full or `maxStoredReports` reached | Check `getStoredReportCount()` and Logcat |
| `Duplicate class androidx...` | Jetifier enabled with mixed artifacts | Set `android.enableJetifier=false` |
| Build fails with `compileSdk 35 required` | `androidx.core` newer than 1.13.1 | Pin `androidx.core:core:1.13.1` or raise `compileSdk` |
