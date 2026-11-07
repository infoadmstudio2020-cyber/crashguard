package com.roni.crashguard.demo.app;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.roni.library.crashguard.CrashGuard;
import com.roni.library.crashguard.CrashReport;

import java.util.List;

/**
 * ডিস্কে সংরক্ষিত সব ক্র্যাশ রিপোর্টের তালিকা।
 */
public class CrashReportsActivity extends AppCompatActivity {

    private CrashReportAdapter adapter;
    private ListView listView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_reports);

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

        listView = findViewById(R.id.list_reports);
        emptyView = findViewById(R.id.tv_empty);

        adapter = new CrashReportAdapter(this);
        listView.setAdapter(adapter);
        listView.setEmptyView(emptyView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CrashReport report = adapter.getItemAt(position);
                Intent intent = new Intent(CrashReportsActivity.this, CrashReportDetailActivity.class);
                intent.putExtra(CrashReportDetailActivity.EXTRA_REPORT_ID, report.getReportId());
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(adapter.getItemAt(position));
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<CrashReport> reports = CrashGuard.getStoredReports();
        adapter.submit(reports);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(getString(R.string.demo_reports_count, reports.size()));
        }
    }

    private void confirmDelete(final CrashReport report) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.demo_delete_title)
            .setMessage(getString(R.string.demo_delete_message, report.getExceptionClass()))
            .setPositiveButton(R.string.demo_delete_confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CrashGuard.deleteReport(report.getReportId());
                    reload();
                }
            })
            .setNegativeButton(R.string.demo_cancel, null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reports, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share_all) {
            shareAll();
            return true;
        }
        if (id == R.id.action_clear_all) {
            confirmClearAll();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareAll() {
        List<CrashReport> reports = CrashGuard.getStoredReports();
        if (reports.isEmpty()) {
            Toast.makeText(this, R.string.demo_no_reports, Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (CrashReport report : reports) {
            sb.append(report.toFormattedString()).append('\n');
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CrashGuard reports (" + reports.size() + ")");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.demo_share_all)));
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.demo_clear_title)
            .setMessage(R.string.demo_clear_message)
            .setPositiveButton(R.string.demo_delete_confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CrashGuard.clearReports();
                    reload();
                }
            })
            .setNegativeButton(R.string.demo_cancel, null)
            .show();
    }
}
