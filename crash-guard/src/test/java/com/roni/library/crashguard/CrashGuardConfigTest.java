package com.roni.library.crashguard;

import org.junit.Assert;
import org.junit.Test;

public class CrashGuardConfigTest {

    @Test
    public void testDefaultConfigValues() {
        CrashGuardConfig config = CrashGuardConfig.defaultConfig();
        Assert.assertTrue(config.isEnabled());
        Assert.assertTrue(config.isDebugMode());
        Assert.assertTrue(config.isShowCrashActivity());
        Assert.assertFalse(config.isAutoRestart());
        Assert.assertTrue(config.isEnableBreadcrumbs());
        Assert.assertTrue(config.isFilterSensitiveData());
        Assert.assertTrue(config.getMaxStoredReports() > 0);
        Assert.assertEquals("CrashGuard", config.getTag());
        Assert.assertNull(config.getCustomCrashActivityClass());
        Assert.assertNull(config.getCrashListener());
    }

    @Test
    public void testCustomBuilderSettings() {
        CrashGuardConfig config = CrashGuardConfig.newBuilder()
            .setEnabled(true)
            .setDebugMode(false)
            .setAutoRestart(true)
            .setRestartDelayMs(2000L)
            .setMaxStoredReports(15)
            .setMaxBreadcrumbs(30)
            .setFilterSensitiveData(false)
            .build();

        Assert.assertTrue(config.isEnabled());
        Assert.assertFalse(config.isDebugMode());
        Assert.assertTrue(config.isAutoRestart());
        Assert.assertEquals(2000L, config.getRestartDelayMs());
        Assert.assertEquals(15, config.getMaxStoredReports());
        Assert.assertEquals(30, config.getMaxBreadcrumbs());
        Assert.assertFalse(config.isFilterSensitiveData());
    }

    @Test
    public void testTagIsNormalised() {
        Assert.assertEquals("CrashGuard", CrashGuardConfig.newBuilder().setTag(null).build().getTag());
        Assert.assertEquals("CrashGuard", CrashGuardConfig.newBuilder().setTag("   ").build().getTag());
        Assert.assertEquals("demo-app", CrashGuardConfig.newBuilder().setTag("  demo-app  ").build().getTag());
    }

    @Test
    public void testDebugEnabledAlias() {
        CrashGuardConfig config = CrashGuardConfig.newBuilder().setDebugEnabled(false).build();
        Assert.assertFalse(config.isDebugMode());
    }

    @Test
    public void testBuilderClampsUnsafeValues() {
        CrashGuardConfig config = CrashGuardConfig.newBuilder()
            .setMaxStoredReports(0)
            .setMaxBreadcrumbs(1)
            .setRestartDelayMs(0L)
            .build();

        Assert.assertEquals(1, config.getMaxStoredReports());
        Assert.assertEquals(5, config.getMaxBreadcrumbs());
        Assert.assertEquals(200L, config.getRestartDelayMs());
    }

    @Test
    public void testToStringContainsKeyFields() {
        String text = CrashGuardConfig.newBuilder().setTag("unit-test").build().toString();
        Assert.assertTrue(text.contains("unit-test"));
        Assert.assertTrue(text.contains("maxStoredReports"));
    }
}
