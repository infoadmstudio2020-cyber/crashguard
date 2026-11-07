package com.roni.library.crashguard;

import android.content.Context;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Thread-safe persistent disk storage for crash reports adhering to FIFO retention limits.
 *
 * <p>Writes are atomic: the payload is first written to a {@code .tmp} file and then
 * renamed over the final name. A process killed mid-write therefore never leaves a
 * truncated report behind.</p>
 */
public class CrashReportStorage {

    private static final String DIR_NAME = "crashguard_reports";
    private static final String FILE_PREFIX = "crash_";
    private static final String FILE_SUFFIX = ".json";
    private static final String TMP_SUFFIX = ".tmp";

    private final Context context;
    private final int maxReports;
    private final Object fileLock = new Object();

    public CrashReportStorage(Context context, int maxReports) {
        this.context = context.getApplicationContext();
        this.maxReports = Math.max(1, maxReports);
    }

    private File getReportsDir() {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) {
            // Fall back to the app files dir so a report is never silently lost.
            return context.getFilesDir();
        }
        return dir;
    }

    /** Absolute path of the directory holding the persisted reports. */
    public File getStorageDir() {
        return getReportsDir();
    }

    public boolean saveReport(CrashReport report) {
        if (report == null || report.getReportId() == null) return false;
        synchronized (fileLock) {
            File tmpFile = null;
            try {
                File dir = getReportsDir();
                File reportFile = new File(dir, FILE_PREFIX + report.getReportId() + FILE_SUFFIX);
                tmpFile = new File(dir, FILE_PREFIX + report.getReportId() + FILE_SUFFIX + TMP_SUFFIX);

                String jsonStr = report.toJson().toString(2);

                try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                    fos.write(jsonStr.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                    fos.getFD().sync();
                }

                if (!tmpFile.renameTo(reportFile)) {
                    // Some file systems refuse to rename over an existing file.
                    //noinspection ResultOfMethodCallIgnored
                    reportFile.delete();
                    if (!tmpFile.renameTo(reportFile)) {
                        //noinspection ResultOfMethodCallIgnored
                        tmpFile.delete();
                        return false;
                    }
                }

                pruneOldReports();
                return true;
            } catch (Exception e) {
                if (tmpFile != null && tmpFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tmpFile.delete();
                }
                return false;
            }
        }
    }

    public List<CrashReport> getAllReports() {
        synchronized (fileLock) {
            List<CrashReport> reports = new ArrayList<>();
            File dir = getReportsDir();
            File[] files = dir.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
            if (files == null || files.length == 0) return reports;

            for (File f : files) {
                CrashReport r = readReportFile(f);
                if (r != null) {
                    reports.add(r);
                }
            }

            // Sort newest first
            Collections.sort(reports, new Comparator<CrashReport>() {
                @Override
                public int compare(CrashReport o1, CrashReport o2) {
                    return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                }
            });

            return reports;
        }
    }

    public CrashReport getLatestReport() {
        List<CrashReport> all = getAllReports();
        return all.isEmpty() ? null : all.get(0);
    }

    /** Loads a single report by id, or {@code null} when it is missing/corrupt. */
    public CrashReport getReport(String reportId) {
        if (reportId == null) return null;
        synchronized (fileLock) {
            File file = new File(getReportsDir(), FILE_PREFIX + reportId + FILE_SUFFIX);
            if (!file.exists()) return null;
            return readReportFile(file);
        }
    }

    public int getReportCount() {
        synchronized (fileLock) {
            File[] files = getReportsDir()
                .listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
            return files == null ? 0 : files.length;
        }
    }

    public boolean deleteReport(String reportId) {
        if (reportId == null) return false;
        synchronized (fileLock) {
            File dir = getReportsDir();
            File file = new File(dir, FILE_PREFIX + reportId + FILE_SUFFIX);
            if (file.exists()) {
                return file.delete();
            }
            return false;
        }
    }

    public void clearAllReports() {
        synchronized (fileLock) {
            File dir = getReportsDir();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        }
    }

    /** Concatenates every stored report into a single shareable text blob. */
    public String exportAllAsText() {
        List<CrashReport> reports = getAllReports();
        if (reports.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("CrashGuard export - ").append(reports.size()).append(" report(s)\n\n");
        for (CrashReport report : reports) {
            sb.append(report.toFormattedString()).append("\n");
        }
        return sb.toString();
    }

    private CrashReport readReportFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject json = new JSONObject(sb.toString());
            return CrashReport.fromJson(json);
        } catch (Exception e) {
            return null;
        }
    }

    private void pruneOldReports() {
        File dir = getReportsDir();
        File[] files = dir.listFiles((d, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX));
        if (files == null || files.length <= maxReports) return;

        // Sort ascending by lastModified (oldest first).
        // NOTE: Comparator.comparingLong() is API 24+, so an explicit comparator
        // is required to stay compatible with minSdk 21.
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.compare(o1.lastModified(), o2.lastModified());
            }
        });

        int toRemove = files.length - maxReports;
        for (int i = 0; i < toRemove; i++) {
            //noinspection ResultOfMethodCallIgnored
            files[i].delete();
        }
    }
}
