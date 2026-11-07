package com.roni.library.crashguard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Structured diagnostic crash report capturing exception, system environment,
 * device state, thread information, and recent breadcrumbs.
 *
 * <p>Instances are plain data holders. They are serialisable so they can be
 * persisted as JSON and (for small payloads) passed through an {@code Intent}.</p>
 */
public class CrashReport implements Serializable {
    private static final long serialVersionUID = 2L;

    /** Report produced by an uncaught exception that terminated the process. */
    public static final String TYPE_UNCAUGHT = "UNCAUGHT";
    /** Report produced by {@code CrashGuard.logHandledException(...)}. */
    public static final String TYPE_HANDLED = "HANDLED";

    private String reportId;
    private long timestamp;
    private String formattedDate;
    private String reportType = TYPE_UNCAUGHT;
    private String tag;
    private String exceptionClass;
    private String exceptionMessage;
    private String stackTrace;
    private String cause;
    private String threadName;
    private long threadId;
    private boolean isMainThread;

    private String appPackage;
    private String appVersionName;
    private long appVersionCode;

    private String androidVersion;
    private int sdkInt;
    private String deviceManufacturer;
    private String deviceModel;
    private String deviceProduct;
    private boolean isRooted;

    private long freeRamMb;
    private long totalRamMb;
    private long freeStorageMb;
    private long totalStorageMb;

    private List<Breadcrumb> breadcrumbs = new ArrayList<>();
    private Map<String, String> customData = new HashMap<>();

    public CrashReport() {
        this.reportId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.formattedDate = format(this.timestamp);
    }

    private static String format(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        return sdf.format(new Date(timestamp));
    }

    public static String getStackTraceString(Throwable throwable) {
        if (throwable == null) return "";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.formattedDate = format(timestamp);
    }

    public String getFormattedDate() { return formattedDate != null ? formattedDate : format(timestamp); }

    public String getReportType() { return reportType != null ? reportType : TYPE_UNCAUGHT; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public boolean isHandledReport() { return TYPE_HANDLED.equals(getReportType()); }

    public String getTag() { return tag != null ? tag : ""; }
    public void setTag(String tag) { this.tag = tag; }

    public String getExceptionClass() { return exceptionClass != null ? exceptionClass : ""; }
    public void setExceptionClass(String exceptionClass) { this.exceptionClass = exceptionClass; }

    public String getExceptionMessage() { return exceptionMessage != null ? exceptionMessage : ""; }
    public void setExceptionMessage(String exceptionMessage) { this.exceptionMessage = exceptionMessage; }

    public String getStackTrace() { return stackTrace != null ? stackTrace : ""; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public String getCause() { return cause != null ? cause : ""; }
    public void setCause(String cause) { this.cause = cause; }

    public String getThreadName() { return threadName != null ? threadName : ""; }
    public void setThreadName(String threadName) { this.threadName = threadName; }

    public long getThreadId() { return threadId; }
    public void setThreadId(long threadId) { this.threadId = threadId; }

    public boolean isMainThread() { return isMainThread; }
    public void setMainThread(boolean mainThread) { isMainThread = mainThread; }

    public String getAppPackage() { return appPackage != null ? appPackage : ""; }
    public void setAppPackage(String appPackage) { this.appPackage = appPackage; }

    public String getAppVersionName() { return appVersionName != null ? appVersionName : ""; }
    public void setAppVersionName(String appVersionName) { this.appVersionName = appVersionName; }

    public long getAppVersionCode() { return appVersionCode; }
    public void setAppVersionCode(long appVersionCode) { this.appVersionCode = appVersionCode; }

    public String getAndroidVersion() { return androidVersion != null ? androidVersion : ""; }
    public void setAndroidVersion(String androidVersion) { this.androidVersion = androidVersion; }

    public int getSdkInt() { return sdkInt; }
    public void setSdkInt(int sdkInt) { this.sdkInt = sdkInt; }

    public String getDeviceManufacturer() { return deviceManufacturer != null ? deviceManufacturer : ""; }
    public void setDeviceManufacturer(String deviceManufacturer) { this.deviceManufacturer = deviceManufacturer; }

    public String getDeviceModel() { return deviceModel != null ? deviceModel : ""; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public String getDeviceProduct() { return deviceProduct != null ? deviceProduct : ""; }
    public void setDeviceProduct(String deviceProduct) { this.deviceProduct = deviceProduct; }

    public boolean isRooted() { return isRooted; }
    public void setRooted(boolean rooted) { isRooted = rooted; }

    public long getFreeRamMb() { return freeRamMb; }
    public void setFreeRamMb(long freeRamMb) { this.freeRamMb = freeRamMb; }

    public long getTotalRamMb() { return totalRamMb; }
    public void setTotalRamMb(long totalRamMb) { this.totalRamMb = totalRamMb; }

    public long getFreeStorageMb() { return freeStorageMb; }
    public void setFreeStorageMb(long freeStorageMb) { this.freeStorageMb = freeStorageMb; }

    public long getTotalStorageMb() { return totalStorageMb; }
    public void setTotalStorageMb(long totalStorageMb) { this.totalStorageMb = totalStorageMb; }

    public List<Breadcrumb> getBreadcrumbs() { return breadcrumbs; }
    public void setBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        this.breadcrumbs = breadcrumbs != null ? new ArrayList<>(breadcrumbs) : new ArrayList<>();
    }

    public Map<String, String> getCustomData() { return customData; }
    public void setCustomData(Map<String, String> customData) {
        this.customData = customData != null ? new HashMap<>(customData) : new HashMap<>();
    }

    /** One-line summary used by list screens. */
    public String getSummary() {
        String message = getExceptionMessage();
        if (message.length() > 120) {
            message = message.substring(0, 117) + "...";
        }
        return getExceptionClass() + (message.isEmpty() ? "" : ": " + message);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("reportId", reportId);
            json.put("timestamp", timestamp);
            json.put("formattedDate", getFormattedDate());
            json.put("reportType", getReportType());
            json.put("tag", getTag());
            json.put("exceptionClass", getExceptionClass());
            json.put("exceptionMessage", getExceptionMessage());
            json.put("stackTrace", getStackTrace());
            json.put("cause", getCause());
            json.put("threadName", getThreadName());
            json.put("threadId", threadId);
            json.put("isMainThread", isMainThread);

            json.put("appPackage", getAppPackage());
            json.put("appVersionName", getAppVersionName());
            json.put("appVersionCode", appVersionCode);

            json.put("androidVersion", getAndroidVersion());
            json.put("sdkInt", sdkInt);
            json.put("deviceManufacturer", getDeviceManufacturer());
            json.put("deviceModel", getDeviceModel());
            json.put("deviceProduct", getDeviceProduct());
            json.put("isRooted", isRooted);

            json.put("freeRamMb", freeRamMb);
            json.put("totalRamMb", totalRamMb);
            json.put("freeStorageMb", freeStorageMb);
            json.put("totalStorageMb", totalStorageMb);

            JSONArray bArr = new JSONArray();
            if (breadcrumbs != null) {
                for (Breadcrumb b : breadcrumbs) {
                    bArr.put(b.toJson());
                }
            }
            json.put("breadcrumbs", bArr);

            JSONObject cdObj = new JSONObject();
            if (customData != null) {
                for (Map.Entry<String, String> entry : customData.entrySet()) {
                    cdObj.put(entry.getKey(), entry.getValue());
                }
            }
            json.put("customData", cdObj);

        } catch (JSONException e) {
            // JSONObject only throws for non-finite numbers or null keys; both
            // are impossible here, but a report must never crash the handler.
        }
        return json;
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public static CrashReport fromJson(JSONObject json) {
        if (json == null) return null;
        CrashReport report = new CrashReport();
        report.setReportId(json.optString("reportId", UUID.randomUUID().toString()));
        report.setTimestamp(json.optLong("timestamp", System.currentTimeMillis()));
        report.setReportType(json.optString("reportType", TYPE_UNCAUGHT));
        report.setTag(json.optString("tag", ""));
        report.setExceptionClass(json.optString("exceptionClass", ""));
        report.setExceptionMessage(json.optString("exceptionMessage", ""));
        report.setStackTrace(json.optString("stackTrace", ""));
        report.setCause(json.optString("cause", ""));
        report.setThreadName(json.optString("threadName", ""));
        report.setThreadId(json.optLong("threadId", 0));
        report.setMainThread(json.optBoolean("isMainThread", false));

        report.setAppPackage(json.optString("appPackage", ""));
        report.setAppVersionName(json.optString("appVersionName", ""));
        report.setAppVersionCode(json.optLong("appVersionCode", 0));

        report.setAndroidVersion(json.optString("androidVersion", ""));
        report.setSdkInt(json.optInt("sdkInt", 0));
        report.setDeviceManufacturer(json.optString("deviceManufacturer", ""));
        report.setDeviceModel(json.optString("deviceModel", ""));
        report.setDeviceProduct(json.optString("deviceProduct", ""));
        report.setRooted(json.optBoolean("isRooted", false));

        report.setFreeRamMb(json.optLong("freeRamMb", 0));
        report.setTotalRamMb(json.optLong("totalRamMb", 0));
        report.setFreeStorageMb(json.optLong("freeStorageMb", 0));
        report.setTotalStorageMb(json.optLong("totalStorageMb", 0));

        JSONArray bArr = json.optJSONArray("breadcrumbs");
        if (bArr != null) {
            List<Breadcrumb> bList = new ArrayList<>();
            for (int i = 0; i < bArr.length(); i++) {
                JSONObject bJson = bArr.optJSONObject(i);
                if (bJson != null) {
                    Breadcrumb b = Breadcrumb.fromJson(bJson);
                    if (b != null) bList.add(b);
                }
            }
            report.setBreadcrumbs(bList);
        }

        JSONObject cdObj = json.optJSONObject("customData");
        if (cdObj != null) {
            Map<String, String> cdMap = new HashMap<>();
            Iterator<String> keys = cdObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                cdMap.put(key, cdObj.optString(key, ""));
            }
            report.setCustomData(cdMap);
        }

        return report;
    }

    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("================ CRASHGUARD REPORT ================\n");
        sb.append("ID: ").append(getReportId()).append("\n");
        sb.append("Type: ").append(getReportType()).append("\n");
        if (!getTag().isEmpty()) {
            sb.append("Tag: ").append(getTag()).append("\n");
        }
        sb.append("Date: ").append(getFormattedDate()).append("\n");
        sb.append("Exception: ").append(getExceptionClass()).append("\n");
        sb.append("Message: ").append(getExceptionMessage()).append("\n");
        sb.append("Thread: ").append(getThreadName()).append(" (id: ").append(threadId)
            .append(", isMain: ").append(isMainThread).append(")\n");
        sb.append("App: ").append(getAppPackage()).append(" v").append(getAppVersionName())
            .append(" (").append(appVersionCode).append(")\n");
        sb.append("Device: ").append(getDeviceManufacturer()).append(" ").append(getDeviceModel())
            .append(" [SDK ").append(sdkInt).append(" - Android ").append(getAndroidVersion()).append("]\n");
        sb.append("Rooted: ").append(isRooted ? "YES" : "NO").append("\n");
        sb.append("RAM: Free ").append(freeRamMb).append("MB / Total ").append(totalRamMb).append("MB\n");
        sb.append("Storage: Free ").append(freeStorageMb).append("MB / Total ").append(totalStorageMb).append("MB\n");

        if (customData != null && !customData.isEmpty()) {
            sb.append("\n--- Custom Metadata ---\n");
            for (Map.Entry<String, String> entry : customData.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        if (breadcrumbs != null && !breadcrumbs.isEmpty()) {
            sb.append("\n--- Recent Breadcrumbs (").append(breadcrumbs.size()).append(") ---\n");
            for (Breadcrumb b : breadcrumbs) {
                sb.append(b.toString()).append("\n");
            }
        }

        sb.append("\n--- Stack Trace ---\n");
        sb.append(getStackTrace()).append("\n");
        sb.append("===================================================\n");
        return sb.toString();
    }
}
