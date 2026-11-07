# Changelog

All notable changes to CrashGuard are documented here.
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] — 2026-11-07

First release that actually builds, publishes and passes its own test suite.
The previous 1.0.0 archive did not compile; this entry documents the full repair.

### Added

- `CrashGuard.uninstall()` / `CrashGuard.release()` — restores the previously
  installed `Thread.UncaughtExceptionHandler`.
- `CrashGuard.getLatestReport()` and `CrashGuard.getStoredReportCount()`.
- `CrashReportStorage.getReport(String)`, `getReportCount()`, `getStorageDir()`,
  `exportAllAsText()`.
- `CrashReport.TYPE_UNCAUGHT` / `TYPE_HANDLED`, `getReportType()`, `isHandledReport()`.
- `CrashReport.getTag()` and `CrashGuardConfig.setTag()`.
- `CrashReport.getSummary()` — one-line description for list UIs.
- `CrashReport.toJsonString()`.
- `CrashNotificationManager.cancel(Context)`.
- Bengali crash-screen translations (`values-bn/strings.xml`).
- Redesigned crash screen: header card, device/session card, breadcrumbs card,
  scrollable stack trace, delete action.
- Demo app: `DemoApplication`, six crash scenarios, diagnostics panel, live status card,
  `CrashReportsActivity`, `CrashReportDetailActivity`, `CrashReportAdapter`.
- `integration-test/` — standalone consumer project that resolves the published AAR.
- `docs/CRASH_TESTING_GUIDE.md`.
- `FINAL_AUDIT_REPORT.md`, `TEST_REPORT.md`.
- Robolectric 4.12.2 + `androidx.test:core:1.6.1` test dependencies.
- `CrashReportStorageTest` (9 tests) and `CrashGuardInstallTest` (10 tests).
- `BreadcrumbTest` (5 tests).
- GitHub Packages publishing repository (conditional on CI environment variables).

### Changed

- **Build:** `compileSdk` / `targetSdk` unified at **34** via the root `ext` block;
  `minSdk` 21; Java 17 toolchain.
- **Build:** `androidx.core` pinned to **1.13.1** (1.15.0 requires compileSdk 35).
- **Build:** `android.enableJetifier=false`, `android.nonTransitiveRClass=true`.
- **Build:** `appcompat` and `material` promoted from `implementation` to `api` — the
  AAR exposes `AppCompatActivity` and `Theme.Material3.*` publicly.
- **Build:** `withJavadocJar()` removed (unreliable for Android libraries);
  `withSourcesJar()` retained.
- **Build:** `testOptions.unitTests.returnDefaultValues` and
  `includeAndroidResources` enabled.
- **Publishing:** coordinates corrected to `com.roni.library:crash-guard:1.0.0`
  (previously `com.roni.crashguard.demo:com.roni.library.crashguard`).
- **Publishing:** publication reads `GROUP` / `POM_ARTIFACT_ID` / `VERSION_NAME`
  from `gradle.properties` instead of hardcoding them.
- **Publishing:** full POM metadata (name, description, url, license, developer, SCM).
- **Publishing:** `jitpack.yml` publishes only the library module.
- **Publishing:** `scripts/build.sh` and `scripts/publish.sh` rewritten.
- **CI:** `ci.yml` replaced by `build.yml` and `test.yml`; `release.yml` rewritten.
- **CI:** all `||` fallback chains removed — failures now fail the job.
- **CI:** explicit `permissions:` blocks, `concurrency` groups, artifact uploads.
- **CI:** release workflow verifies `tag == VERSION_NAME` before publishing.
- **Runtime:** crash activity receives only `reportId` across the process boundary and
  reloads the report from disk (fixes `TransactionTooLargeException`).
- **Runtime:** report persistence is atomic (`.tmp` → `fsync` → rename).
- **Runtime:** `customData` is snapshotted before being attached to a report.
- **Runtime:** `CrashHandler` guards against self-referencing previous handlers.
- **Runtime:** crash-activity grace period raised from 150 ms to 400 ms.
- **Runtime:** `logHandledException()` now records the full environment snapshot.
- **Runtime:** `DefaultCrashActivity` migrated to `OnBackPressedDispatcher`.
- **Runtime:** notification path checks `areNotificationsEnabled()` and uses `BigTextStyle`.
- **Demo:** theme migrated to `Theme.Material3.DayNight.NoActionBar`.
- **Demo:** `buildFeatures { buildConfig true }` enabled.
- **Demo:** CrashGuard installed in `Application.onCreate()` with a main-process guard.
- **Demo:** full Bengali UI.
- **Docs:** README rewritten with correct coordinates, API tables and build commands.

### Fixed

- Demo app imported `com.roni.crashguard.demo.*` instead of `com.roni.library.crashguard.*`
  (compile error).
- Demo app called `new CrashGuardConfig.Builder()` (private constructor — compile error).
- Demo app called non-existent `debugEnabled()`, `tag()`, `init()`, `isInitialized()`
  (compile errors).
- `compileSdk 36` with AGP 8.3.2 (unsupported — build error).
- `androidx.core:1.15.0` with `compileSdk 34` (build error).
- `Demo.SectionHeader` style had no explicit parent → AAPT link failure.
- `Comparator.comparingLong` used with `minSdk 21` → `NoSuchMethodError` on API 21–23
  (found by `lintRelease`).
- Init breadcrumb was dropped because the constructor used the static helper before the
  singleton was assigned.
- Full `CrashReport` was passed through a Binder transaction → `TransactionTooLargeException`.
- Live `ConcurrentHashMap` was attached to persisted reports.
- `uninstall()` did not exist, so the previous handler could never be restored.
- Truncated JSON files could be left behind by an interrupted write.
- Crash activity had no disk fallback when the Intent payload was missing.
- Notification silently never appeared on Android 13+ without `POST_NOTIFICATIONS`.
- ProGuard rules referenced a package that does not exist in the library module.
- Consumer rules were missing keeps for the `Serializable` contract and internal classes.
- `scripts/build.sh` referenced a non-existent `:java-library` module.
- README documented a Maven coordinate that could never resolve.

### Removed

- `.github/workflows/ci.yml` (replaced by `build.yml` + `test.yml`).
- `withJavadocJar()` from the release publication.
- All `||` fallback chains from CI workflows.

### Security

- Sensitive-data filtering masks `password`, `token`, `authorization`, `secret` and
  `api_key` values in breadcrumbs and custom data before they are written to disk.
- Reports are stored in app-private internal storage only.
- No network permission is requested by the library.

---

## [Unreleased]

### Planned

- Instrumented tests (`androidTest`) for the crash activity and notification flow.
- Optional report encryption at rest.
- Pluggable report uploader interface.
- `compileSdk` 35 migration once AGP 8.6+ is adopted.
