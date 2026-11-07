package com.roni.crashguard.demo.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.roni.library.crashguard.Breadcrumb;
import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashGuardConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * ডেমো অ্যাপের মূল স্ক্রিন।
 *
 * <p>এখান থেকে বিভিন্ন ধরনের ক্র্যাশ পরিস্থিতি পরীক্ষা করা যায় এবং
 * CrashGuard-এর পাবলিক API ব্যবহার করে দেখা যায়।</p>
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATIONS = 1001;

    private TextView tvStatus;
    private TextView tvConfig;
    private TextView tvBreadcrumbCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvStatus = findViewById(R.id.tv_status);
        tvConfig = findViewById(R.id.tv_config);
        tvBreadcrumbCount = findViewById(R.id.tv_breadcrumb_count);

        bindCrashButtons();
        bindDiagnosticButtons();

        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    // ------------------------------------------------------------------
    // ক্র্যাশ পরীক্ষার বাটন
    // ------------------------------------------------------------------

    private void bindCrashButtons() {
        Button btnNpe = findViewById(R.id.btn_crash_npe);
        Button btnBackground = findViewById(R.id.btn_crash_background);
        Button btnStackOverflow = findViewById(R.id.btn_crash_stack_overflow);
        Button btnIllegalState = findViewById(R.id.btn_crash_illegal_state);
        Button btnOom = findViewById(R.id.btn_crash_oom);
        Button btnHandled = findViewById(R.id.btn_handled_exception);

        btnNpe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("NullPointerException ট্রিগার করা হচ্ছে", Breadcrumb.Type.USER_ACTION, "test");
                triggerNullPointer();
            }
        });

        btnBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("ব্যাকগ্রাউন্ড থ্রেড ক্র্যাশ ট্রিগার করা হচ্ছে", Breadcrumb.Type.USER_ACTION, "test");
                triggerBackgroundCrash();
            }
        });

        btnStackOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("StackOverflowError ট্রিগার করা হচ্ছে", Breadcrumb.Type.USER_ACTION, "test");
                triggerStackOverflow();
            }
        });

        btnIllegalState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("IllegalStateException ট্রিগার করা হচ্ছে", Breadcrumb.Type.USER_ACTION, "test");
                throw new IllegalStateException("ডেমো: ইচ্ছাকৃতভাবে তৈরি করা IllegalStateException");
            }
        });

        btnOom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("OutOfMemoryError ট্রিগার করা হচ্ছে", Breadcrumb.Type.USER_ACTION, "test");
                triggerOutOfMemory();
            }
        });

        btnHandled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    throw new IllegalArgumentException("ডেমো: নন-ফ্যাটাল এক্সেপশন");
                } catch (IllegalArgumentException e) {
                    CrashGuard.logHandledException(e);
                }
                Toast.makeText(MainActivity.this, R.string.demo_handled_logged, Toast.LENGTH_SHORT).show();
                refreshStatus();
            }
        });
    }

    /** মেইন থ্রেডে NullPointerException — প্রসেস বন্ধ হবে এবং ক্র্যাশ স্ক্রিন আসবে। */
    private void triggerNullPointer() {
        String value = null;
        //noinspection ResultOfMethodCallIgnored
        value.length();
    }

    /** ব্যাকগ্রাউন্ড থ্রেডে RuntimeException — নোটিফিকেশন ও ক্র্যাশ স্ক্রিন আসবে। */
    private void triggerBackgroundCrash() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("ডেমো: ব্যাকগ্রাউন্ড থ্রেডে ইচ্ছাকৃত ক্র্যাশ");
            }
        }, "demo-background-thread").start();
    }

    /** StackOverflowError — গভীর রিকার্সন। */
    private void triggerStackOverflow() {
        recurse(0);
    }

    private int recurse(int depth) {
        return recurse(depth + 1) + 1;
    }

    /** OutOfMemoryError — নিয়ন্ত্রিত মেমোরি চাপ। */
    private void triggerOutOfMemory() {
        List<long[]> hog = new ArrayList<>();
        while (true) {
            hog.add(new long[1024 * 1024]);
        }
    }

    // ------------------------------------------------------------------
    // ডায়াগনস্টিক বাটন
    // ------------------------------------------------------------------

    private void bindDiagnosticButtons() {
        Button btnAddBreadcrumb = findViewById(R.id.btn_add_breadcrumb);
        Button btnCustomKey = findViewById(R.id.btn_custom_key);
        Button btnViewReports = findViewById(R.id.btn_view_reports);
        Button btnClearReports = findViewById(R.id.btn_clear_reports);

        btnAddBreadcrumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.addBreadcrumb("ম্যানুয়াল ব্রেডক্রাম্ব #" + System.currentTimeMillis(),
                    Breadcrumb.Type.CUSTOM, "manual");
                Toast.makeText(MainActivity.this, R.string.demo_breadcrumb_added, Toast.LENGTH_SHORT).show();
                refreshStatus();
            }
        });

        btnCustomKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.setCustomKey("last_action", "custom_key_" + System.currentTimeMillis());
                CrashGuard.setCustomKey("user_tier", "premium");
                Toast.makeText(MainActivity.this, R.string.demo_custom_key_set, Toast.LENGTH_SHORT).show();
            }
        });

        btnViewReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CrashReportsActivity.class));
            }
        });

        btnClearReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CrashGuard.clearReports();
                Toast.makeText(MainActivity.this, R.string.demo_reports_cleared, Toast.LENGTH_SHORT).show();
                refreshStatus();
            }
        });
    }

    // ------------------------------------------------------------------
    // স্ট্যাটাস
    // ------------------------------------------------------------------

    private void refreshStatus() {
        boolean installed = CrashGuard.isInstalled();
        int reportCount = CrashGuard.getStoredReportCount();

        tvStatus.setText(getString(R.string.demo_status_line,
            installed ? getString(R.string.demo_status_installed) : getString(R.string.demo_status_not_installed),
            reportCount));

        CrashGuardConfig config = CrashGuard.getInstance() != null
            ? CrashGuard.getInstance().getConfig() : null;

        if (config != null) {
            tvConfig.setText(getString(R.string.demo_config_line,
                config.getTag(),
                config.isDebugMode() ? "ON" : "OFF",
                config.isAutoRestart() ? "ON" : "OFF",
                config.getMaxStoredReports(),
                config.getMaxBreadcrumbs()));
        } else {
            tvConfig.setText(R.string.demo_config_unavailable);
        }

        int breadcrumbs = CrashGuard.getInstance() != null
            ? CrashGuard.getInstance().getBreadcrumbTracker().size() : 0;
        tvBreadcrumbCount.setText(getString(R.string.demo_breadcrumb_line, breadcrumbs));
    }

    // ------------------------------------------------------------------
    // Android 13+ নোটিফিকেশন পারমিশন
    // ------------------------------------------------------------------

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
            this,
            new String[]{Manifest.permission.POST_NOTIFICATIONS},
            REQUEST_NOTIFICATIONS
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS && grantResults.length > 0
            && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.demo_notification_permission_denied, Toast.LENGTH_LONG).show();
        }
    }
}
