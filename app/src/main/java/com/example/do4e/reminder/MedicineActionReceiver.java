package com.example.do4e.reminder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.do4e.db.AppDataBase;

public class MedicineActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int medId = intent.getIntExtra("med_id", -1);
        int notifId = intent.getIntExtra("notif_id", -1); // Extract notifId

        if (medId != -1) {
            new Thread(() -> {
                AppDataBase.getInstance(context).medDAO().incrementDaysTaken(medId);
                NotificationManager manager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                if (notifId != -1) {
                    manager.cancel(notifId); // Use notifId for cancellation
                }
            }).start();
        }
    }
}