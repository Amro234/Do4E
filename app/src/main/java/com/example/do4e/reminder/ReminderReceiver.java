package com.example.do4e.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.example.do4e.R;

public class ReminderReceiver extends BroadcastReceiver {

        private static final String CHANNEL_ID = "med_reminder_silent_channel";
        private static final String TAG = "ReminderReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
                String medName = intent.getStringExtra("med_name");
                int notifId = intent.getIntExtra("notif_id", 0);
                int medId = intent.getIntExtra("med_id", -1);

                android.util.Log.d(TAG, "🔔 Alarm fired! med=" + medName
                                + " medId=" + medId + " notifId=" + notifId);

                // ── Delegate Alarm and Notification to Foreground Service ──
                Intent serviceIntent = new Intent(context, AlarmSoundService.class);
                serviceIntent.putExtra("med_name", medName);
                serviceIntent.putExtra("notif_id", notifId);
                serviceIntent.putExtra("med_id", medId);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                } else {
                        context.startService(serviceIntent);
                }

                // ── Re-schedule for the same time tomorrow ────────────────────────
                ReminderScheduler.rescheduleNextDay(context, intent);
        }
}