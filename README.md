# CrashGuard

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache-2.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)

A production-ready Android crash guard library. It intercepts uncaught exceptions,
captures a structured diagnostic report, persists it to disk, and shows a recovery
screen from an **isolated process** so the UI survives the death of the host process.

---

## Features

| Feature | Description |
|---|---|
| Uncaught exception interception | Replaces `Thread.setDefaultUncaughtExceptionHandler` and chains to the previous handler |
| Main + background thread coverage | Detects the crashing thread and records `isMainThread` |
| Structured reports | Exception, stack trace, cause, thread, app version, device, RAM, storage, root state |
| Breadcrumbs | Thread-safe ring buffer of the last *N* user actions / navigation / network events |
| Custom metadata | `setCustomKey()` key/value pairs attached to every report |
| Persistent storage | Atomic JSON files in internal storage with FIFO retention |
| Isolated recovery UI | `DefaultCrashActivity` runs in `:crash_guard_process` |
| Background crash notification | High-priority notification with a deep link to the report |
| Non-fatal logging | `logCrashHandledException()` for caught exceptions |
| Sensitive data filtering | Masks passwords and bearer tokens before persisting |
| Bengali + English UI | `values/` and `values-bn/` crash screen translations |

---

## Installation

### From JitPack

```gradle
// settings.gradle
dependencyResolutionManagement {
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
    implementation 'com.github.infoadmstudio2020-cyber.crash-guard:crash-guard:1.0.0'
}
```

### From Maven Local (after `./gradlew :crash-guard:publishReleasePublicationToMavenLocal`)

```gradle
repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    implementation 'com.roni.library:crash-guard:1.0.0'
}
```

### From GitHub Packages

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/infoadmstudio2020-cyber/crash-guard")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'com.roni.library:crash-guard:1.0.0'
}
```

### From source

```gradle
dependencies {
    implementation project(':crash-guard')
}
```

---

## Usage

### 1. Install in `Application.onCreate()`

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
            .setCrashListener((thread, throwable, report) -> {
                // Upload the report to your backend here.
            })
            .build();

        CrashGuard.install(this, config);
    }
}
```

> Install from `Application`, not from an `Activity`, so crashes during the first
> screen are already covered.

### 2. Record breadcrumbs

```java
CrashGuard.addBreadcrumb("User opened checkout");
CrashGuard.addBreadcrumb("Payment API called", Breadcrumb.Type.NETWORK, "payments");
```

### 3. Attach custom metadata

```java
CrashGuard.setCustomKey("user_tier", "premium");
CrashGuard.setCustomKey("last_screen", "CheckoutActivity");
```

### 4. Log non-fatal exceptions

```java
try {
    riskyOperation();
} catch (IOException e) {
    CrashGuard.logHandledException(e);   // stored, app keeps running
}
```

### 5. Read stored reports

```java
List<CrashReport> reports = CrashGuard.getStoredReports();   // newest first
CrashReport latest = CrashGuard.getLatestReport();
int count = CrashGuard.getStoredReportCount();

String text = latest != null ? latest.toFormattedString() : "";
```

### 6. Delete reports

```java
CrashGuard.deleteReport(reportId);
CrashGuard.clearReports();
```

### 7. Uninstall

```java
CrashGuard.uninstall();   // restores the previous uncaught exception handler
```

---

## Public API

### `CrashGuard`

| Method | Description |
|---|---|
| `install(Context)` | Install with default config |
| `install(Context, CrashGuardConfig)` | Install with custom config (idempotent) |
| `getInstance()` | Current instance or `null` |
| `isInstalled()` / `isInitialized()` | Installation state |
| `uninstall()` / `release()` | Restore the previous handler and reset state |
| `addBreadcrumb(String)` | Add a user-action breadcrumb |
| `addBreadcrumb(String, Breadcrumb.Type, String)` | Add a typed breadcrumb |
| `setCustomKey(String, String)` | Set/remove (`null`) a metadata key |
| `logHandledException(Throwable)` | Persist a non-fatal report |
| `getStoredReports()` | All reports, newest first |
| `getLatestReport()` | Most recent report or `null` |
| `getStoredReportCount()` | Number of persisted reports |
| `deleteReport(String)` | Delete one report |
| `clearReports()` | Delete all reports |
| `getConfig()` / `getStorage()` / `getBreadcrumbTracker()` | Accessors |

### `CrashGuardConfig.Builder`

| Method | Default |
|---|---|
| `setEnabled(boolean)` | `true` |
| `setDebugMode(boolean)` / `setDebugEnabled(boolean)` | `true` |
| `setTag(String)` | `"CrashGuard"` |
| `setShowCrashActivity(boolean)` | `true` |
| `setCustomCrashActivityClass(Class)` | `null` (uses `DefaultCrashActivity`) |
| `setRestartActivityClass(Class)` | `null` (uses launcher intent) |
| `setAutoRestart(boolean)` | `false` |
| `setRestartDelayMs(long)` | `1200` (min `200`) |
| `setMaxStoredReports(int)` | `25` (min `1`) |
| `setEnableBreadcrumbs(boolean)` | `true` |
| `setMaxBreadcrumbs(int)` | `50` (min `5`) |
| `setFilterSensitiveData(boolean)` | `true` |
| `setShowNotificationOnBackgroundCrash(boolean)` | `true` |
| `setCrashListener(CrashListener)` | `null` |

### `CrashReport`

`getReportId()`, `getTimestamp()`, `getFormattedDate()`, `getReportType()`
(`UNCAUGHT` / `HANDLED`), `getTag()`, `getExceptionClass()`, `getExceptionMessage()`,
`getStackTrace()`, `getCause()`, `getThreadName()`, `getThreadId()`, `isMainThread()`,
`getAppPackage()`, `getAppVersionName()`, `getAppVersionCode()`, `getAndroidVersion()`,
`getSdkInt()`, `getDeviceManufacturer()`, `getDeviceModel()`, `getDeviceProduct()`,
`isRooted()`, `getFreeRamMb()`, `getTotalRamMb()`, `getFreeStorageMb()`,
`getTotalStorageMb()`, `getBreadcrumbs()`, `getCustomData()`, `getSummary()`,
`toJson()`, `toJsonString()`, `toFormattedString()`, `fromJson(JSONObject)`.

---

## Custom crash screen

```java
CrashGuardConfig.newBuilder()
    .setCustomCrashActivityClass(MyCrashActivity.class)
    .build();
```

`MyCrashActivity` receives `DefaultCrashActivity.EXTRA_REPORT_ID`; load the payload with
`CrashGuard.getInstance().getStorage().getReport(reportId)`.

---

## Requirements

- `minSdk` 21, `compileSdk` 34
- Java 17
- AndroidX (`android.useAndroidX=true`)

The library manifest already declares `POST_NOTIFICATIONS`. On Android 13+ the host app
must still request it at runtime for background-crash notifications to appear.

---

## Build & test

```bash
./gradlew :crash-guard:assembleRelease          # AAR
./gradlew :crash-guard:testDebugUnitTest        # unit tests
./gradlew :app:assembleDebug                    # demo app
./gradlew :crash-guard:lintRelease              # lint
./gradlew :crash-guard:publishReleasePublicationToMavenLocal
```

See [`docs/SETUP.md`](docs/SETUP.md), [`docs/API.md`](docs/API.md) and
[`docs/CRASH_TESTING_GUIDE.md`](docs/CRASH_TESTING_GUIDE.md).

---

## Project layout

```
crash-guard/        Android library module (the published artifact)
app/                Demo application with crash test buttons and a report viewer
integration-test/   Standalone app that consumes the published AAR from mavenLocal
docs/               Setup, API and crash-testing guides
scripts/            build.sh / publish.sh helpers
.github/workflows/  build.yml, test.yml, release.yml
```

---

## License

```
Copyright 2026 MD Roni Hossain

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
