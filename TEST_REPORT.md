# CrashGuard — Test Report

**Date:** 2026-11-07
**Environment:** Linux x86_64, JDK 17.0.13 (Temurin), Gradle 8.7, AGP 8.3.2,
Android SDK Platform 34, Build-Tools 34.0.0.
**Legend:** PASS = executed and succeeded · FAIL = executed and failed ·
NOT RUN = not executed · BLOCKED = cannot be executed in this environment.

---

## 1. Summary

| Category | Total | PASS | FAIL | NOT RUN | BLOCKED |
|---|---|---|---|---|---|
| Gradle / configuration | 4 | 4 | 0 | 0 | 0 |
| Builds | 5 | 5 | 0 | 0 | 0 |
| Unit tests | 47 | 47 | 0 | 0 | 0 |
| Static analysis | 2 | 2 | 0 | 0 | 0 |
| Publishing | 6 | 6 | 0 | 0 | 0 |
| Config validation | 2 | 2 | 0 | 0 | 0 |
| Runtime / device | 4 | 0 | 0 | 0 | 4 |
| CI / external | 3 | 0 | 0 | 0 | 3 |
| **Total** | **73** | **66** | **0** | **0** | **7** |

---

## 2. Gradle configuration tests

| # | Test | Status | Evidence |
|---|---|---|---|
| G-1 | Root `settings.gradle` evaluates (pluginManagement + dependencyResolutionManagement) | **PASS** | `BUILD SUCCESSFUL` |
| G-2 | Both modules included (`:crash-guard`, `:app`) | **PASS** | Task graph contains both |
| G-3 | `gradle.properties` coordinates parsed by the publication | **PASS** | POM contains `com.roni.library:crash-guard:1.0.0` |
| G-4 | `FAIL_ON_PROJECT_REPOS` enforced (no module-level repositories) | **PASS** | Configuration phase succeeded |

---

## 3. Build tests

| # | Test | Command | Status | Output |
|---|---|---|---|---|
| B-1 | Library release AAR | `:crash-guard:assembleRelease` | **PASS** | `crash-guard-release.aar` — 47,419 bytes |
| B-2 | Demo debug APK | `:app:assembleDebug` | **PASS** | `app-debug.apk` — 5,994,072 bytes |
| B-3 | Demo release APK | `:app:assembleRelease` | **PASS** | `app-release.apk` — 4,927,750 bytes |
| B-4 | Clean build from scratch | `clean` + all of the above | **PASS** | `BUILD SUCCESSFUL` |
| B-5 | Consumer app against published AAR | `integration-test :app:assembleDebug` | **PASS** | `app-debug.apk` — 5,751,791 bytes |

### Build failures encountered and fixed during this audit

| Attempt | Failure | Fix |
|---|---|---|
| 1 | `AAPT: resource style/Demo not found` | `Demo.SectionHeader` had no explicit parent → implicit `Demo` parent. Added `parent=""`. |
| 2 | `DemoApplication.java:37: cannot find symbol BuildConfig` | AGP 8.x does not generate `BuildConfig` by default. Added `buildFeatures { buildConfig true }`. |
| 3 | `BreadcrumbTest.java:33: unreported exception JSONException` | `org.json:json` (JVM) declares `JSONException` as checked. Added `throws Exception`. |
| 4 | `lintRelease` — `Comparator.comparingLong` requires API 24 | Replaced with an explicit `Comparator<File>` (real API-21 crash bug). |

---

## 4. Unit tests

Command: `./gradlew :crash-guard:testDebugUnitTest`
Result: **47 tests, 0 failures, 0 errors, 0 skipped.**

| Test class | Tests | Status | Coverage |
|---|---|---|---|
| `CrashGuardConfigTest` | 6 | **PASS** | Defaults, builder, tag normalisation, `setDebugEnabled` alias, value clamping, `toString` |
| `CrashReportTest` | 11 | **PASS** | JSON round-trip, stack-trace extraction (incl. `null`), report type, tag, breadcrumbs, custom data, defensive copy, summary truncation, formatted output, timestamp, `fromJson(null)` |
| `BreadcrumbTest` | 5 | **PASS** | JSON round-trip, null safety, unknown-type fallback, `fromJson(null)`, `toString` |
| `BreadcrumbTrackerTest` | 6 | **PASS** | Ring-buffer eviction, clear, minimum capacity, null rejection, snapshot isolation, JSON array |
| `CrashReportStorageTest` (Robolectric, SDK 34) | 9 | **PASS** | Save/read, newest-first ordering, FIFO retention, delete, clear, latest, **no `.tmp` leftovers**, export, null rejection |
| `CrashGuardInstallTest` (Robolectric, SDK 34) | 10 | **PASS** | Install + idempotency, handler registration, uninstall restores previous handler, init breadcrumb, breadcrumb/custom-key helpers, handled-exception persistence, null rejection, delete/clear, pre-install safety, null-context rejection, version |

### Raw results

```
TEST-com.roni.library.crashguard.BreadcrumbTest.xml          tests="5"  failures="0" errors="0"
TEST-com.roni.library.crashguard.BreadcrumbTrackerTest.xml   tests="6"  failures="0" errors="0"
TEST-com.roni.library.crashguard.CrashGuardConfigTest.xml    tests="6"  failures="0" errors="0"
TEST-com.roni.library.crashguard.CrashGuardInstallTest.xml   tests="10" failures="0" errors="0"
TEST-com.roni.library.crashguard.CrashReportStorageTest.xml  tests="9"  failures="0" errors="0"
TEST-com.roni.library.crashguard.CrashReportTest.xml         tests="11" failures="0" errors="0"
```

---

## 5. Static analysis

| # | Test | Command | Status | Result |
|---|---|---|---|---|
| S-1 | Android Lint (release) | `:crash-guard:lintRelease` | **PASS** | 0 errors |
| S-2 | Java compilation warnings | `compileReleaseJavaWithJavac` | **PASS** | No errors |

Lint found one genuine defect that the original CI never checked:

```
CrashReportStorage.java:205: Error: Call requires API level 24 (current min is 21):
java.util.Comparator#comparingLong [NewApi]
```

This would have thrown `NoSuchMethodError` on Android 5.0–6.0 whenever the report
retention limit was exceeded. Fixed and re-verified.

---

## 6. Publishing tests

| # | Test | Status | Evidence |
|---|---|---|---|
| P-1 | `publishReleasePublicationToMavenLocal` | **PASS** | Task executed |
| P-2 | AAR published | **PASS** | `crash-guard-1.0.0.aar` (47,419 B) |
| P-3 | POM published and valid | **PASS** | `crash-guard-1.0.0.pom` (2,281 B) — correct group/artifact/version/packaging/license/developer/SCM |
| P-4 | Sources JAR published | **PASS** | `crash-guard-1.0.0-sources.jar` (21,821 B) |
| P-5 | Gradle module metadata published | **PASS** | `crash-guard-1.0.0.module` (3,562 B) |
| P-6 | Consumer resolves the artifact | **PASS** | `integration-test` compiled and packaged against `mavenLocal()` |

### Generated POM (excerpt)

```xml
<groupId>com.roni.library</groupId>
<artifactId>crash-guard</artifactId>
<version>1.0.0</version>
<packaging>aar</packaging>
<name>CrashGuard</name>
<licenses><license><name>The Apache License, Version 2.0</name></license></licenses>
<dependencies>
  <dependency><groupId>androidx.appcompat</groupId><artifactId>appcompat</artifactId>
    <version>1.7.0</version><scope>compile</scope></dependency>
  <dependency><groupId>com.google.android.material</groupId><artifactId>material</artifactId>
    <version>1.12.0</version><scope>compile</scope></dependency>
  <dependency><groupId>androidx.core</groupId><artifactId>core</artifactId>
    <version>1.13.1</version><scope>runtime</scope></dependency>
</dependencies>
```

---

## 7. Configuration validation

| # | Test | Status | Result |
|---|---|---|---|
| C-1 | YAML parse — `.github/workflows/build.yml`, `test.yml`, `release.yml`, `jitpack.yml` | **PASS** | 4/4 parsed |
| C-2 | XML parse — all manifests, layouts, drawables, menus, values | **PASS** | 28/28 parsed |

---

## 8. Tests that could not be executed

| # | Test | Status | Reason / required environment |
|---|---|---|---|
| R-1 | Crash guard runtime — main-thread crash → isolated crash screen | **BLOCKED** | Requires an Android device or emulator. No `emulator`/`adb` device is available in this sandbox. |
| R-2 | Crash guard runtime — background-thread crash → notification | **BLOCKED** | Same as R-1; also needs `POST_NOTIFICATIONS` granted on API 33+. |
| R-3 | App restart / recovery behaviour | **BLOCKED** | Same as R-1. |
| R-4 | Instrumented tests (`androidTest`) | **BLOCKED** | No emulator. The module is configured for them (`androidx.test` + Espresso) but no `androidTest` sources exist yet. |
| E-1 | GitHub Actions execution | **BLOCKED** | Requires pushing to a GitHub repository with Actions enabled. Workflows were validated statically (YAML + task names + permissions). |
| E-2 | JitPack build | **BLOCKED** | Requires a public GitHub repository and a pushed `v*` tag. |
| E-3 | GitHub Packages publish | **BLOCKED** | Requires a `GITHUB_TOKEN` with `packages: write`. |

### How to run the blocked tests

```bash
# R-1..R-3: install the demo and press the crash buttons
./gradlew :app:installDebug
adb shell am start -n com.roni.crashguard.demo.app.debug/com.roni.crashguard.demo.app.MainActivity

# R-4: instrumented tests (once androidTest sources exist)
./gradlew :crash-guard:connectedDebugAndroidTest

# E-1..E-3: push and tag
git push origin main
git tag v1.0.0 && git push origin v1.0.0
```

---

## 9. Regression checks performed after each fix

| Fix | Re-verified |
|---|---|
| SDK level alignment | `:crash-guard:assembleRelease` + `:app:assembleDebug` |
| `buildConfig true` | `:app:assembleDebug` |
| Style parent | `:app:assembleDebug` (AAPT link) |
| API-21 comparator | `:crash-guard:lintRelease` + `:crash-guard:testDebugUnitTest` |
| Atomic storage | `CrashReportStorageTest.testNoTemporaryFilesRemainAfterSave` |
| Handler restore | `CrashGuardInstallTest.testUninstallRestoresPreviousHandler` |
| Report id intent path | `CrashReportStorageTest.testSaveAndReadBack` + `testLatestReport` |
| POM coordinates | `publishReleasePublicationToMavenLocal` + `integration-test` build |

---

## 10. Verdict

- **Build:** PASS — library AAR, demo debug APK and demo release APK all produced.
- **Tests:** PASS — 47/47 unit tests green, including Robolectric coverage of the
  on-disk store and the install/uninstall lifecycle.
- **Lint:** PASS — 0 errors; one real API-21 crash bug found and fixed.
- **Publishing:** PASS — valid AAR + POM + sources, consumed successfully by a
  standalone app.
- **Runtime on device:** NOT VERIFIED — no device/emulator available. This is the only
  remaining gap and it is explicitly **not** reported as a pass.
