package com.example.do4e.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.do4e.R;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "med_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String medName = intent.getStringExtra("med_name");
        int notifId = intent.getIntExtra("notif_id", 0);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Required for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medication Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Daily medication reminders");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_plus_24px)
                .setContentTitle("💊 Medication Reminder")
                .setContentText("Time to take: " + medName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(notifId, builder.build());

        ReminderScheduler.rescheduleNextDay(context, intent);
    }
}