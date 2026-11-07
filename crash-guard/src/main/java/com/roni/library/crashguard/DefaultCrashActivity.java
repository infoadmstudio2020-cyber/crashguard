package com.roni.library.crashguard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Isolated process Activity providing a robust diagnostic interface when the main app crashes.
 *
 * <p>The activity runs in {@code :crash_guard_process}, therefore it survives the death of
 * the main process. Only the report id is received through the Intent; the payload itself
 * is reloaded from disk.</p>
 */
public class DefaultCrashActivity extends AppCompatActivity {

    /** Legacy extra kept for source compatibility. Prefer {@link #EXTRA_REPORT_ID}. */
    public static final String EXTRA_CRASH_REPORT = "extra_crashguard_report";
    public static final String EXTRA_REPORT_ID = "extra_crashguard_report_id";
    public static final String EXTRA_RESTART_ACTIVITY = "extra_restart_activity";

    private CrashReport crashReport;
    private String restartActivityClassName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_crash);

        Intent intent = getIntent();
        if (intent != null) {
            restartActivityClassName = intent.getStringExtra(EXTRA_RESTART_ACTIVITY);
            crashReport = loadReport(intent);
        }

        if (crashReport == null) {
            // Last resort: show whatever is on disk so the screen is never empty.
            crashReport = new CrashReportStorage(this, 25).getLatestReport();
        }

        initViews();
        registerBackHandler();
    }

    private CrashReport loadReport(Intent intent) {
        String reportId = intent.getStringExtra(EXTRA_REPORT_ID);
        if (reportId != null) {
            CrashReport loaded = new CrashReportStorage(this, 25).getReport(reportId);
            if (loaded != null) return loaded;
        }

        // Backwards compatible path for hosts that still pass the serialisable extra.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return intent.getSerializableExtra(EXTRA_CRASH_REPORT, CrashReport.class);
            }
            //noinspection deprecation
            Object legacy = intent.getSerializableExtra(EXTRA_CRASH_REPORT);
            return legacy instanceof CrashReport ? (CrashReport) legacy : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void registerBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                closeApplication();
            }
        });
    }

    private void initViews() {
        TextView tvExceptionName = findViewById(R.id.tv_exception_name);
        TextView tvExceptionMessage = findViewById(R.id.tv_exception_message);
        TextView tvMeta = findViewById(R.id.tv_meta);
        TextView tvBreadcrumbs = findViewById(R.id.tv_breadcrumbs);
        TextView tvStackTrace = findViewById(R.id.tv_stacktrace);
        Button btnCopy = findViewById(R.id.btn_copy);
        Button btnShare = findViewById(R.id.btn_share);
        Button btnRestart = findViewById(R.id.btn_restart);
        Button btnClose = findViewById(R.id.btn_close);
        TextView btnDelete = findViewById(R.id.btn_delete);

        if (crashReport != null) {
            tvExceptionName.setText(crashReport.getExceptionClass());
            tvExceptionMessage.setText(crashReport.getExceptionMessage());
            tvMeta.setText(buildMetaText(crashReport));
            tvBreadcrumbs.setText(buildBreadcrumbText(crashReport));
            tvStackTrace.setText(crashReport.getStackTrace());
        } else {
            tvExceptionName.setText(R.string.crashguard_report_unavailable);
            tvExceptionMessage.setText(R.string.crashguard_report_unavailable_desc);
            tvMeta.setText("");
            tvBreadcrumbs.setText("");
            tvStackTrace.setText("");
        }

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyReportToClipboard();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareCrashReport();
            }
        });

        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApplication();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeApplication();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCurrentReport();
            }
        });
    }

    private String buildMetaText(CrashReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.crashguard_meta_time)).append(": ").append(report.getFormattedDate()).append('\n');
        sb.append(getString(R.string.crashguard_meta_type)).append(": ").append(report.getReportType()).append('\n');
        sb.append(getString(R.string.crashguard_meta_thread)).append(": ").append(report.getThreadName())
            .append(report.isMainThread() ? " (main)" : "").append('\n');
        sb.append(getString(R.string.crashguard_meta_app)).append(": ").append(report.getAppPackage())
            .append(" v").append(report.getAppVersionName()).append('\n');
        sb.append(getString(R.string.crashguard_meta_device)).append(": ")
            .append(report.getDeviceManufacturer()).append(' ').append(report.getDeviceModel()).append('\n');
        sb.append(getString(R.string.crashguard_meta_os)).append(": Android ")
            .append(report.getAndroidVersion()).append(" (SDK ").append(report.getSdkInt()).append(")\n");
        sb.append(getString(R.string.crashguard_meta_ram)).append(": ")
            .append(report.getFreeRamMb()).append(" / ").append(report.getTotalRamMb()).append(" MB\n");
        sb.append(getString(R.string.crashguard_meta_storage)).append(": ")
            .append(report.getFreeStorageMb()).append(" / ").append(report.getTotalStorageMb()).append(" MB");
        return sb.toString();
    }

    private String buildBreadcrumbText(CrashReport report) {
        if (report.getBreadcrumbs().isEmpty()) {
            return getString(R.string.crashguard_no_breadcrumbs);
        }
        StringBuilder sb = new StringBuilder();
        for (Breadcrumb breadcrumb : report.getBreadcrumbs()) {
            sb.append(breadcrumb.toString()).append('\n');
        }
        return sb.toString().trim();
    }

    private void copyReportToClipboard() {
        if (crashReport == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("CrashGuard Report", crashReport.toFormattedString());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.crashguard_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCrashReport() {
        if (crashReport == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Report: " + crashReport.getExceptionClass());
        shareIntent.putExtra(Intent.EXTRA_TEXT, crashReport.toFormattedString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.crashguard_btn_share)));
    }

    private void deleteCurrentReport() {
        if (crashReport != null) {
            new CrashReportStorage(this, 25).deleteReport(crashReport.getReportId());
            Toast.makeText(this, R.string.crashguard_report_deleted, Toast.LENGTH_SHORT).show();
        }
        closeApplication();
    }

    private void restartApplication() {
        Intent restartIntent = null;
        if (restartActivityClassName != null && !restartActivityClassName.isEmpty()) {
            try {
                Class<?> clazz = Class.forName(restartActivityClassName);
                restartIntent = new Intent(this, clazz);
            } catch (ClassNotFoundException ignored) {
                // Fall through to the launcher intent.
            }
        }

        if (restartIntent == null) {
            PackageManager pm = getPackageManager();
            restartIntent = pm.getLaunchIntentForPackage(getPackageName());
        }

        if (restartIntent != null) {
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(restartIntent);
        }

        finish();
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    private void closeApplication() {
        finish();
        Process.killProcess(Process.myPid());
        System.exit(10);
    }
}
