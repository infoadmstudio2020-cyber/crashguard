package com.roni.library.crashguard;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Verifies the public install / uninstall lifecycle and the static helper API.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CrashGuardInstallTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        CrashGuard.uninstall();
        CrashGuard.clearReports();
    }

    @After
    public void tearDown() {
        CrashGuard.clearReports();
        CrashGuard.uninstall();
    }

    @Test
    public void testInstallRegistersHandlerAndIsIdempotent() {
        Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();

        CrashGuard first = CrashGuard.install(context, CrashGuardConfig.newBuilder().setTag("t1").build());
        Assert.assertNotNull(first);
        Assert.assertTrue(CrashGuard.isInstalled());
        Assert.assertTrue(CrashGuard.isInitialized());
        Assert.assertSame(first, CrashGuard.install(context));
        Assert.assertSame(first, CrashGuard.getInstance());

        Thread.UncaughtExceptionHandler installed = Thread.getDefaultUncaughtExceptionHandler();
        Assert.assertTrue(installed instanceof CrashHandler);
        Assert.assertNotSame(before, installed);
    }

    @Test
    public void testUninstallRestoresPreviousHandler() {
        Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
        CrashGuard.install(context);
        CrashGuard.uninstall();

        Assert.assertFalse(CrashGuard.isInstalled());
        Assert.assertNull(CrashGuard.getInstance());
        Assert.assertSame(before, Thread.getDefaultUncaughtExceptionHandler());
    }

    @Test
    public void testInitialBreadcrumbIsRecorded() {
        CrashGuard guard = CrashGuard.install(context);
        Assert.assertTrue(guard.getBreadcrumbTracker().size() >= 1);
        Assert.assertTrue(guard.getBreadcrumbTracker().getAll().get(0).getMessage().contains("initialized"));
    }

    @Test
    public void testBreadcrumbAndCustomKeyHelpers() {
        CrashGuard guard = CrashGuard.install(context);
        int before = guard.getBreadcrumbTracker().size();

        CrashGuard.addBreadcrumb("hello");
        Assert.assertEquals(before + 1, guard.getBreadcrumbTracker().size());

        CrashGuard.setCustomKey("k", "v");
        CrashGuard.setCustomKey("k", null);
        Assert.assertNotNull(guard.getConfig());
    }

    @Test
    public void testLogHandledExceptionPersistsReport() {
        CrashGuard.install(context, CrashGuardConfig.newBuilder().setTag("handled-test").build());

        CrashGuard.logHandledException(new IllegalStateException("handled boom"));

        List<CrashReport> reports = CrashGuard.getStoredReports();
        Assert.assertEquals(1, reports.size());

        CrashReport report = reports.get(0);
        Assert.assertEquals(CrashReport.TYPE_HANDLED, report.getReportType());
        Assert.assertTrue(report.isHandledReport());
        Assert.assertEquals("handled-test", report.getTag());
        Assert.assertTrue(report.getExceptionMessage().contains("handled boom"));
        Assert.assertEquals("java.lang.IllegalStateException", report.getExceptionClass());
        Assert.assertNotNull(report.getAppPackage());
        Assert.assertTrue(report.getSdkInt() > 0);
    }

    @Test
    public void testLogHandledExceptionIgnoresNull() {
        CrashGuard.install(context);
        CrashGuard.logHandledException(null);
        Assert.assertEquals(0, CrashGuard.getStoredReportCount());
    }

    @Test
    public void testDeleteAndClearReports() {
        CrashGuard.install(context);
        CrashGuard.logHandledException(new RuntimeException("one"));
        CrashGuard.logHandledException(new RuntimeException("two"));

        List<CrashReport> reports = CrashGuard.getStoredReports();
        Assert.assertEquals(2, reports.size());

        Assert.assertTrue(CrashGuard.deleteReport(reports.get(0).getReportId()));
        Assert.assertEquals(1, CrashGuard.getStoredReportCount());

        CrashGuard.clearReports();
        Assert.assertEquals(0, CrashGuard.getStoredReportCount());
        Assert.assertNull(CrashGuard.getLatestReport());
    }

    @Test
    public void testStaticHelpersAreSafeBeforeInstall() {
        Assert.assertFalse(CrashGuard.isInstalled());
        Assert.assertTrue(CrashGuard.getStoredReports().isEmpty());
        Assert.assertEquals(0, CrashGuard.getStoredReportCount());
        Assert.assertNull(CrashGuard.getLatestReport());
        Assert.assertFalse(CrashGuard.deleteReport("x"));
        CrashGuard.clearReports();
        CrashGuard.addBreadcrumb("no-op");
        CrashGuard.setCustomKey("no-op", "value");
        CrashGuard.logHandledException(new RuntimeException("no-op"));
    }

    @Test
    public void testInstallRejectsNullContext() {
        try {
            CrashGuard.install(null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("Context"));
        }
    }

    @Test
    public void testLibraryVersion() {
        CrashGuard guard = CrashGuard.install(context);
        Assert.assertEquals("1.0.1", guard.getLibraryVersion());
    }
}
