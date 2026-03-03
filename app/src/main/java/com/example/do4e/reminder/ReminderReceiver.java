package com.example.do4e.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.do4e.R;
import com.example.do4e.ui.notification.med_alarm;

public class ReminderReceiver extends BroadcastReceiver {

        private static final String CHANNEL_ID = "med_reminder_channel";
        private static final String TAG = "ReminderReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
                String medName = intent.getStringExtra("med_name");
                int notifId = intent.getIntExtra("notif_id", 0);
                int medId = intent.getIntExtra("med_id", -1);

                android.util.Log.d(TAG, "🔔 Alarm fired! med=" + medName
                                + " medId=" + medId + " notifId=" + notifId);

                NotificationManager manager = (NotificationManager) context
                                .getSystemService(Context.NOTIFICATION_SERVICE);

                // ── Create / update notification channel (Android 8+) ────────────
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(
                                        CHANNEL_ID,
                                        "Medication Reminders",
                                        NotificationManager.IMPORTANCE_HIGH);
                        channel.setDescription("Daily medication reminders");
                        channel.enableVibration(true);
                        channel.setBypassDnd(true); // pierce Do Not Disturb
                        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                        manager.createNotificationChannel(channel);
                }

                // ── "Mark as Taken" action (dismisses notification + logs dose) ───
                Intent takenIntent = new Intent(context, MedicineActionReceiver.class);
                takenIntent.putExtra("med_id", medId);
                takenIntent.putExtra("notif_id", notifId);
                PendingIntent takenPendingIntent = PendingIntent.getBroadcast(
                                context,
                                notifId,
                                takenIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                // ── Full-screen alarm activity intent ─────────────────────────────
                Intent fullScreenIntent = new Intent(context, med_alarm.class);
                fullScreenIntent.putExtra("med_id", medId);
                fullScreenIntent.putExtra("notif_id", notifId);
                fullScreenIntent.putExtra("med_name", medName);
                fullScreenIntent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                                context,
                                // Use a different request code than takenIntent to avoid PendingIntent
                                // collision
                                notifId + 10000,
                                fullScreenIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                // ── Build notification ────────────────────────────────────────────
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                                .setSmallIcon(R.drawable.pill_24dp_icon)
                                .setContentTitle("Medicine Time!")
                                .setContentText("Time to take " + medName)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setCategory(NotificationCompat.CATEGORY_ALARM) // ← key for lock-screen priority
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setAutoCancel(true)
                                // Tapping the notification body opens the full-screen alarm UI
                                .setContentIntent(fullScreenPendingIntent)
                                // fullScreenIntent: auto-launches MedAlarmActivity when screen is off / locked
                                .setFullScreenIntent(fullScreenPendingIntent, true)
                                .addAction(R.drawable.check_24dp_icon, "Mark as Taken", takenPendingIntent);

                manager.notify(notifId, builder.build());

                // ── Re-schedule for the same time tomorrow ────────────────────────
                ReminderScheduler.rescheduleNextDay(context, intent);
        }
}