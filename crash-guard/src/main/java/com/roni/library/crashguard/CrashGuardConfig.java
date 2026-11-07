package com.roni.library.crashguard;

import android.app.Activity;

/**
 * Immutable configuration holder and fluent builder for the CrashGuard runtime.
 *
 * <p>Instances are created through {@link #newBuilder()} or {@link #defaultConfig()}.
 * The class is immutable, therefore it is safe to share between threads.</p>
 */
public final class CrashGuardConfig {

    private final boolean enabled;
    private final boolean debugMode;
    private final String tag;
    private final boolean showCrashActivity;
    private final Class<? extends Activity> customCrashActivityClass;
    private final Class<? extends Activity> restartActivityClass;
    private final boolean autoRestart;
    private final long restartDelayMs;
    private final int maxStoredReports;
    private final boolean enableBreadcrumbs;
    private final int maxBreadcrumbs;
    private final boolean filterSensitiveData;
    private final boolean showNotificationOnBackgroundCrash;
    private final CrashListener crashListener;

    private CrashGuardConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.debugMode = builder.debugMode;
        this.tag = builder.tag;
        this.showCrashActivity = builder.showCrashActivity;
        this.customCrashActivityClass = builder.customCrashActivityClass;
        this.restartActivityClass = builder.restartActivityClass;
        this.autoRestart = builder.autoRestart;
        this.restartDelayMs = builder.restartDelayMs;
        this.maxStoredReports = builder.maxStoredReports;
        this.enableBreadcrumbs = builder.enableBreadcrumbs;
        this.maxBreadcrumbs = builder.maxBreadcrumbs;
        this.filterSensitiveData = builder.filterSensitiveData;
        this.showNotificationOnBackgroundCrash = builder.showNotificationOnBackgroundCrash;
        this.crashListener = builder.crashListener;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static CrashGuardConfig defaultConfig() {
        return new Builder().build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Free-form tag attached to every report. Useful when one app ships several
     * flavours or when reports are aggregated across modules.
     */
    public String getTag() {
        return tag;
    }

    public boolean isShowCrashActivity() {
        return showCrashActivity;
    }

    public Class<? extends Activity> getCustomCrashActivityClass() {
        return customCrashActivityClass;
    }

    public Class<? extends Activity> getRestartActivityClass() {
        return restartActivityClass;
    }

    public boolean isAutoRestart() {
        return autoRestart;
    }

    public long getRestartDelayMs() {
        return restartDelayMs;
    }

    public int getMaxStoredReports() {
        return maxStoredReports;
    }

    public boolean isEnableBreadcrumbs() {
        return enableBreadcrumbs;
    }

    public int getMaxBreadcrumbs() {
        return maxBreadcrumbs;
    }

    public boolean isFilterSensitiveData() {
        return filterSensitiveData;
    }

    public boolean isShowNotificationOnBackgroundCrash() {
        return showNotificationOnBackgroundCrash;
    }

    public CrashListener getCrashListener() {
        return crashListener;
    }

    @Override
    public String toString() {
        return "CrashGuardConfig{" +
            "enabled=" + enabled +
            ", debugMode=" + debugMode +
            ", tag='" + tag + '\'' +
            ", showCrashActivity=" + showCrashActivity +
            ", autoRestart=" + autoRestart +
            ", restartDelayMs=" + restartDelayMs +
            ", maxStoredReports=" + maxStoredReports +
            ", enableBreadcrumbs=" + enableBreadcrumbs +
            ", maxBreadcrumbs=" + maxBreadcrumbs +
            ", filterSensitiveData=" + filterSensitiveData +
            ", showNotificationOnBackgroundCrash=" + showNotificationOnBackgroundCrash +
            '}';
    }

    public static class Builder {
        private boolean enabled = true;
        private boolean debugMode = true;
        private String tag = "CrashGuard";
        private boolean showCrashActivity = true;
        private Class<? extends Activity> customCrashActivityClass = null;
        private Class<? extends Activity> restartActivityClass = null;
        private boolean autoRestart = false;
        private long restartDelayMs = 1200L;
        private int maxStoredReports = 25;
        private boolean enableBreadcrumbs = true;
        private int maxBreadcrumbs = 50;
        private boolean filterSensitiveData = true;
        private boolean showNotificationOnBackgroundCrash = true;
        private CrashListener crashListener = null;

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }

        /** Alias of {@link #setDebugMode(boolean)} kept for API ergonomics. */
        public Builder setDebugEnabled(boolean debugEnabled) {
            return setDebugMode(debugEnabled);
        }

        public Builder setTag(String tag) {
            this.tag = (tag == null || tag.trim().isEmpty()) ? "CrashGuard" : tag.trim();
            return this;
        }

        public Builder setShowCrashActivity(boolean showCrashActivity) {
            this.showCrashActivity = showCrashActivity;
            return this;
        }

        public Builder setCustomCrashActivityClass(Class<? extends Activity> customCrashActivityClass) {
            this.customCrashActivityClass = customCrashActivityClass;
            return this;
        }

        public Builder setRestartActivityClass(Class<? extends Activity> restartActivityClass) {
            this.restartActivityClass = restartActivityClass;
            return this;
        }

        public Builder setAutoRestart(boolean autoRestart) {
            this.autoRestart = autoRestart;
            return this;
        }

        public Builder setRestartDelayMs(long restartDelayMs) {
            this.restartDelayMs = Math.max(200L, restartDelayMs);
            return this;
        }

        public Builder setMaxStoredReports(int maxStoredReports) {
            this.maxStoredReports = Math.max(1, maxStoredReports);
            return this;
        }

        public Builder setEnableBreadcrumbs(boolean enableBreadcrumbs) {
            this.enableBreadcrumbs = enableBreadcrumbs;
            return this;
        }

        public Builder setMaxBreadcrumbs(int maxBreadcrumbs) {
            this.maxBreadcrumbs = Math.max(5, maxBreadcrumbs);
            return this;
        }

        public Builder setFilterSensitiveData(boolean filterSensitiveData) {
            this.filterSensitiveData = filterSensitiveData;
            return this;
        }

        public Builder setShowNotificationOnBackgroundCrash(boolean showNotificationOnBackgroundCrash) {
            this.showNotificationOnBackgroundCrash = showNotificationOnBackgroundCrash;
            return this;
        }

        public Builder setCrashListener(CrashListener crashListener) {
            this.crashListener = crashListener;
            return this;
        }

        public CrashGuardConfig build() {
            return new CrashGuardConfig(this);
        }
    }
}
