package com.roni.crashguard.demo.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashReport;

/**
 * একটি নির্দিষ্ট ক্র্যাশ রিপোর্টের বিস্তারিত ভিউ।
 */
public class CrashReportDetailActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "extra_report_id";

    private CrashReport report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_report_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        if (reportId != null && CrashGuard.getInstance() != null) {
            report = CrashGuard.getInstance().getStorage().getReport(reportId);
        }

        TextView tvContent = findViewById(R.id.tv_report_content);
        if (report != null) {
            tvContent.setText(report.toFormattedString());
        } else {
            tvContent.setText(R.string.demo_report_missing);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_report_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_copy) {
            copyToClipboard();
            return true;
        }
        if (id == R.id.action_share) {
            shareReport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyToClipboard() {
        if (report == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("CrashGuard Report", report.toFormattedString()));
            Toast.makeText(this, R.string.demo_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReport() {
        if (report == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash Report: " + report.getExceptionClass());
        shareIntent.putExtra(Intent.EXTRA_TEXT, report.toFormattedString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.demo_share)));
    }
}
