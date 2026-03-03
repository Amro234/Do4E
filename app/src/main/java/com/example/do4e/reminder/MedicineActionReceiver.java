package com.example.do4e.reminder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.do4e.db.AppDataBase;

public class MedicineActionReceiver extends BroadcastReceiver {

    /** Action sent by the "Mark as Taken" button on the status-bar notification. */
    public static final String ACTION_TAKEN = "com.example.do4e.ACTION_TAKEN";

    /** Action sent by the "Skip this dose" link in MedAlarmActivity
     *  (or from a future "Skip" action button on the notification). */
    public static final String ACTION_SKIP  = "com.example.do4e.ACTION_SKIP";

    @Override
    public void onReceive(Context context, Intent intent) {
        int medId   = intent.getIntExtra("med_id",   -1);
        int notifId = intent.getIntExtra("notif_id", -1);
        String action = intent.getAction();

        // Default to "taken" behaviour when action string is absent
        // (backwards-compatible with the existing takenPendingIntent setup
        //  in ReminderReceiver which does not set an explicit action).
        boolean isTaken = action == null || ACTION_TAKEN.equals(action);

        if (isTaken && medId != -1) {
            // Increment daysTaken on a background thread, then cancel notification
            new Thread(() -> {
                AppDataBase.getInstance(context).medDAO().incrementDaysTaken(medId);
                cancelNotification(context, notifId);
            }).start();
        } else {
            // Skip: just cancel the notification without logging the dose
            cancelNotification(context, notifId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void cancelNotification(Context context, int notifId) {
        if (notifId == -1) return;
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(notifId);
    }
}