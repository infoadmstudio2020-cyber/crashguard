package com.roni.integration;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.roni.library.crashguard.Breadcrumb;
import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashGuardConfig;

/**
 * Consumes the published CrashGuard AAR exactly like a third-party app would.
 * If this module compiles and links, the published artifact is usable.
 */
public class IntegrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CrashGuardConfig config = CrashGuardConfig.newBuilder()
            .setDebugMode(BuildConfig.DEBUG)
            .setTag("integration-test")
            .setMaxStoredReports(10)
            .build();

        CrashGuard.install(getApplicationContext(), config);
        CrashGuard.addBreadcrumb("integration activity created", Breadcrumb.Type.NAVIGATION, "ui");
        CrashGuard.setCustomKey("consumer", "integration-test");

        TextView status = new TextView(this);
        status.setText("CrashGuard installed: " + CrashGuard.isInstalled()
            + "\nVersion: " + CrashGuard.getInstance().getLibraryVersion()
            + "\nStored reports: " + CrashGuard.getStoredReportCount());
        setContentView(status);
    }
}
