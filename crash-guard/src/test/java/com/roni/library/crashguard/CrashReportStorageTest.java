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
 * Robolectric-backed tests for the on-disk report store.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CrashReportStorageTest {

    private Context context;
    private CrashReportStorage storage;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        storage = new CrashReportStorage(context, 3);
        storage.clearAllReports();
    }

    @After
    public void tearDown() {
        storage.clearAllReports();
    }

    private CrashReport report(String message, long timestamp) {
        CrashReport report = new CrashReport();
        report.setExceptionClass("java.lang.RuntimeException");
        report.setExceptionMessage(message);
        report.setStackTrace("trace for " + message);
        report.setTimestamp(timestamp);
        return report;
    }

    @Test
    public void testSaveAndReadBack() {
        CrashReport original = report("first", 1000L);
        Assert.assertTrue(storage.saveReport(original));

        CrashReport loaded = storage.getReport(original.getReportId());
        Assert.assertNotNull(loaded);
        Assert.assertEquals("first", loaded.getExceptionMessage());
        Assert.assertEquals(original.getReportId(), loaded.getReportId());
    }

    @Test
    public void testGetAllReportsSortedNewestFirst() {
        storage.saveReport(report("old", 1000L));
        storage.saveReport(report("new", 5000L));
        storage.saveReport(report("middle", 3000L));

        List<CrashReport> all = storage.getAllReports();
        Assert.assertEquals(3, all.size());
        Assert.assertEquals("new", all.get(0).getExceptionMessage());
        Assert.assertEquals("middle", all.get(1).getExceptionMessage());
        Assert.assertEquals("old", all.get(2).getExceptionMessage());
    }

    @Test
    public void testRetentionLimitPrunesOldest() {
        storage.saveReport(report("r1", 1000L));
        storage.saveReport(report("r2", 2000L));
        storage.saveReport(report("r3", 3000L));
        storage.saveReport(report("r4", 4000L));

        Assert.assertEquals(3, storage.getReportCount());
    }

    @Test
    public void testDeleteReport() {
        CrashReport report = report("to-delete", 1000L);
        storage.saveReport(report);

        Assert.assertTrue(storage.deleteReport(report.getReportId()));
        Assert.assertNull(storage.getReport(report.getReportId()));
        Assert.assertFalse(storage.deleteReport("does-not-exist"));
    }

    @Test
    public void testClearAllReports() {
        storage.saveReport(report("a", 1000L));
        storage.saveReport(report("b", 2000L));
        storage.clearAllReports();

        Assert.assertEquals(0, storage.getReportCount());
        Assert.assertTrue(storage.getAllReports().isEmpty());
    }

    @Test
    public void testLatestReport() {
        Assert.assertNull(storage.getLatestReport());
        storage.saveReport(report("older", 1000L));
        storage.saveReport(report("newest", 9000L));

        CrashReport latest = storage.getLatestReport();
        Assert.assertNotNull(latest);
        Assert.assertEquals("newest", latest.getExceptionMessage());
    }

    @Test
    public void testNoTemporaryFilesRemainAfterSave() {
        storage.saveReport(report("atomic", 1000L));
        String[] files = storage.getStorageDir().list();
        Assert.assertNotNull(files);
        for (String name : files) {
            Assert.assertFalse("leftover temp file: " + name, name.endsWith(".tmp"));
        }
    }

    @Test
    public void testExportAllAsText() {
        storage.saveReport(report("exported", 1000L));
        String text = storage.exportAllAsText();
        Assert.assertTrue(text.contains("exported"));
        Assert.assertTrue(text.contains("CRASHGUARD REPORT"));
    }

    @Test
    public void testSaveNullReportIsRejected() {
        Assert.assertFalse(storage.saveReport(null));
    }
}
