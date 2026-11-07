# Crash Testing Guide

How to verify that CrashGuard actually catches, stores and displays crashes.

---

## 1. Install the demo

```bash
./gradlew :app:installDebug
adb shell am start -n com.roni.crashguard.demo.app.debug/com.roni.crashguard.demo.app.MainActivity
```

The status card at the top must read **"অবস্থা: ইনস্টল করা হয়েছে"** with a report count.

---

## 2. Crash scenarios

The demo exposes six buttons.

| Button | What it throws | Thread | Expected result |
|---|---|---|---|
| NullPointerException (মেইন থ্রেড) | `String s = null; s.length();` | Main | Crash screen in `:crash_guard_process` |
| RuntimeException (ব্যাকগ্রাউন্ড থ্রেড) | `throw new RuntimeException(...)` | Background | Notification + report stored |
| StackOverflowError (মেইন থ্রেড) | Unbounded recursion | Main | Crash screen |
| IllegalStateException (মেইন থ্রেড) | `throw new IllegalStateException(...)` | Main | Crash screen |
| OutOfMemoryError (মেমোরি চাপ) | Large array allocation | Main | Crash screen (may be slow) |
| হ্যান্ডেল করা এক্সেপশন (নন-ফ্যাটাল) | `CrashGuard.logHandledException(...)` | Main | **No crash** — report count +1 |

### Verify the main-thread path

1. Press **NullPointerException**.
2. The app process dies and the crash screen opens.
3. Confirm the screen shows: exception class, message, device model, Android version,
   app version, breadcrumbs and the stack trace.
4. Press **সংরক্ষিত রিপোর্ট দেখুন** (View stored reports) to open the list, then tap the entry.
5. Relaunch the app — the report is still listed.

### Verify the background-thread path

1. Press **RuntimeException (ব্যাকগ্রাউন্ড থ্রেড)**.
2. The process dies; a high-priority notification appears.
3. Tap the notification — the crash screen opens with the report.
4. On Android 13+, grant `POST_NOTIFICATIONS` first or no notification will appear.

### Verify the non-fatal path

1. Press **হ্যান্ডেল করা এক্সেপশন (নন-ফ্যাটাল)**.
2. The app keeps running.
3. The report count increases by one.
4. Open the report list — the entry is badged **হ্যান্ডেলড**.

---

## 3. Verify persistence

```bash
adb shell run-as com.roni.crashguard.demo.app.debug ls -la files/crash_reports/
```

Each report is a `<uuid>.json` file. There must be **no `.tmp` files** — a leftover
`.tmp` would mean an interrupted write.

```bash
adb shell run-as com.roni.crashguard.demo.app.debug cat files/crash_reports/<uuid>.json
```

Confirm the JSON contains `reportId`, `timestamp`, `exceptionClass`, `stackTrace`,
`breadcrumbs` and `customData`.

---

## 4. Verify retention

1. Set `setMaxStoredReports(3)` in `DemoApplication`.
2. Trigger four handled exceptions.
3. `getStoredReportCount()` must return **3** and the oldest report must be gone.

---

## 5. Verify the isolated process

```bash
adb shell ps -A | grep crashguard
```

While the crash screen is visible you should see two processes:

```
com.roni.crashguard.demo.app.debug
com.roni.crashguard.demo.app.debug:crash_guard_process
```

The crash screen must survive the death of the main process — that is the whole point
of the separate process.

---

## 6. Verify breadcrumbs

1. Press **ব্রেডক্রাম্ব যোগ করুন** three times.
2. Trigger a crash.
3. The crash screen's breadcrumb card must list the three entries plus the
   `CrashGuard initialized` entry, newest first.

---

## 7. Verify custom keys

1. Press **কাস্টম কী সেট করুন**.
2. Trigger a crash.
3. The report detail screen must show `demo_key = demo_value`.

---

## 8. Verify sensitive-data filtering

```java
CrashGuard.setCustomKey("password", "hunter2");
CrashGuard.setCustomKey("auth_token", "abc123");
```

Trigger a crash and inspect the JSON — both values must be masked (`***`), not stored
in plain text.

---

## 9. Verify uninstall

```java
CrashGuard.uninstall();
```

Then trigger a crash. The system dialog must appear instead of the CrashGuard screen,
proving the previous handler was restored.

---

## 10. Automated checks

```bash
./gradlew :crash-guard:testDebugUnitTest    # 47 unit tests
./gradlew :crash-guard:lintRelease          # 0 errors
./gradlew :app:assembleDebug :app:assembleRelease
```

---

## 11. Common failures

| Symptom | Likely cause |
|---|---|
| Crash screen flashes and the app restarts immediately | `setAutoRestart(true)` with a very short delay |
| Crash screen never appears | `setShowCrashActivity(false)`, or the activity is not declared in the merged manifest |
| Report list empty after a crash | Storage write failed — check Logcat for `CrashGuard` |
| Notification missing on API 33+ | `POST_NOTIFICATIONS` not granted |
| `TransactionTooLargeException` | A custom crash activity reading `EXTRA_CRASH_REPORT` instead of `EXTRA_REPORT_ID` |
| Two `Application.onCreate()` calls | Expected — the crash activity runs in a second process |
