package com.roni.library.crashguard;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class CrashReportTest {

    @Test
    public void testSerializationAndDeserialization() {
        CrashReport report = new CrashReport();
        report.setExceptionClass("java.lang.NullPointerException");
        report.setExceptionMessage("Attempt to invoke virtual method on a null object reference");
        report.setStackTrace("java.lang.NullPointerException\n\tat com.roni.demo.MainActivity.onClick(MainActivity.java:42)");
        report.setThreadName("main");
        report.setThreadId(1);
        report.setMainThread(true);
        report.setAppPackage("com.roni.crashguard.demo");
        report.setAppVersionName("1.0.1");
        report.setDeviceModel("Pixel 8 Pro");
        report.setDeviceManufacturer("Google");

        JSONObject json = report.toJson();
        Assert.assertNotNull(json);

        CrashReport parsed = CrashReport.fromJson(json);
        Assert.assertNotNull(parsed);
        Assert.assertEquals(report.getReportId(), parsed.getReportId());
        Assert.assertEquals(report.getExceptionClass(), parsed.getExceptionClass());
        Assert.assertEquals(report.getExceptionMessage(), parsed.getExceptionMessage());
        Assert.assertEquals(report.getThreadName(), parsed.getThreadName());
        Assert.assertEquals(report.getAppPackage(), parsed.getAppPackage());
        Assert.assertEquals(report.getDeviceModel(), parsed.getDeviceModel());
        Assert.assertTrue(parsed.isMainThread());
    }

    @Test
    public void testStackTraceExtraction() {
        Throwable throwable = new IllegalArgumentException("Invalid test parameter provided");
        String trace = CrashReport.getStackTraceString(throwable);
        Assert.assertTrue(trace.contains("IllegalArgumentException"));
        Assert.assertTrue(trace.contains("Invalid test parameter provided"));
    }

    @Test
    public void testStackTraceExtractionHandlesNull() {
        Assert.assertEquals("", CrashReport.getStackTraceString(null));
    }

    @Test
    public void testReportTypeAndTagRoundTrip() {
        CrashReport report = new CrashReport();
        report.setReportType(CrashReport.TYPE_HANDLED);
        report.setTag("demo-app");
        report.setExceptionClass("java.io.IOException");

        CrashReport parsed = CrashReport.fromJson(report.toJson());
        Assert.assertNotNull(parsed);
        Assert.assertEquals(CrashReport.TYPE_HANDLED, parsed.getReportType());
        Assert.assertTrue(parsed.isHandledReport());
        Assert.assertEquals("demo-app", parsed.getTag());
    }

    @Test
    public void testDefaultReportTypeIsUncaught() {
        CrashReport report = new CrashReport();
        Assert.assertEquals(CrashReport.TYPE_UNCAUGHT, report.getReportType());
        Assert.assertFalse(report.isHandledReport());
    }

    @Test
    public void testBreadcrumbsAndCustomDataRoundTrip() {
        CrashReport report = new CrashReport();
        report.setBreadcrumbs(java.util.Arrays.asList(
            new Breadcrumb("opened screen", Breadcrumb.Type.NAVIGATION, "ui"),
            new Breadcrumb("tapped button", Breadcrumb.Type.USER_ACTION, "ui")
        ));
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("user_tier", "premium");
        report.setCustomData(data);

        CrashReport parsed = CrashReport.fromJson(report.toJson());
        Assert.assertNotNull(parsed);
        Assert.assertEquals(2, parsed.getBreadcrumbs().size());
        Assert.assertEquals("opened screen", parsed.getBreadcrumbs().get(0).getMessage());
        Assert.assertEquals("premium", parsed.getCustomData().get("user_tier"));
    }

    @Test
    public void testCustomDataIsDefensivelyCopied() {
        CrashReport report = new CrashReport();
        java.util.Map<String, String> live = new java.util.HashMap<>();
        live.put("a", "1");
        report.setCustomData(live);
        live.put("b", "2");

        Assert.assertEquals(1, report.getCustomData().size());
    }

    @Test
    public void testSummaryTruncatesLongMessages() {
        CrashReport report = new CrashReport();
        report.setExceptionClass("java.lang.RuntimeException");
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longMessage.append('x');
        }
        report.setExceptionMessage(longMessage.toString());

        String summary = report.getSummary();
        Assert.assertTrue(summary.startsWith("java.lang.RuntimeException: "));
        Assert.assertTrue(summary.endsWith("..."));
        Assert.assertTrue(summary.length() < 160);
    }

    @Test
    public void testFormattedStringContainsSections() {
        CrashReport report = new CrashReport();
        report.setExceptionClass("java.lang.IllegalStateException");
        report.setExceptionMessage("boom");
        report.setStackTrace("trace-line");
        report.setBreadcrumbs(java.util.Collections.singletonList(
            new Breadcrumb("crumb", Breadcrumb.Type.SYSTEM, "test")));

        String text = report.toFormattedString();
        Assert.assertTrue(text.contains("CRASHGUARD REPORT"));
        Assert.assertTrue(text.contains("java.lang.IllegalStateException"));
        Assert.assertTrue(text.contains("Recent Breadcrumbs"));
        Assert.assertTrue(text.contains("trace-line"));
    }

    @Test
    public void testTimestampUpdatesFormattedDate() {
        CrashReport report = new CrashReport();
        report.setTimestamp(0L);
        Assert.assertNotNull(report.getFormattedDate());
        Assert.assertTrue(report.getFormattedDate().startsWith("1970-01-01"));
    }

    @Test
    public void testFromJsonHandlesNull() {
        Assert.assertNull(CrashReport.fromJson(null));
    }
}
