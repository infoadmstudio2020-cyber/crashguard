package com.roni.library.crashguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Handles system notification alerts when crashes occur in background services or non-UI threads.
 */
public final class CrashNotificationManager {

    private static final String CHANNEL_ID = "crashguard_diagnostics_channel";
    private static final int NOTIFICATION_ID = 911911;

    private CrashNotificationManager() {}

    public static void showCrashNotification(Context context, CrashReport report) {
        if (context == null || report == null) return;

        try {
            NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            createChannel(context, nm);

            // Android 13+ requires the runtime POST_NOTIFICATIONS permission.
            // Without it notify() is a silent no-op, so bail out early.
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return;
            }

            Intent intent = new Intent(context, DefaultCrashActivity.class);
            intent.putExtra(DefaultCrashActivity.EXTRA_REPORT_ID, report.getReportId());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pi = PendingIntent.getActivity(context, 0, intent, flags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.crashguard_notification_title))
                .setContentText(report.getExceptionClass() + ": " + report.getExceptionMessage())
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(report.getExceptionClass() + ": " + report.getExceptionMessage()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

            nm.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception ignored) {
            // Notifications may be restricted by permission or by the OEM.
        }
    }

    /** Removes the crash notification, e.g. after the user reviewed the report. */
    public static void cancel(Context context) {
        if (context == null) return;
        try {
            NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
        } catch (Exception ignored) {
            // Nothing to do.
        }
    }

    private static void createChannel(Context context, NotificationManager nm) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.crashguard_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.crashguard_notification_channel_desc));
        channel.enableVibration(true);
        nm.createNotificationChannel(channel);
    }
}
