package com.example.do4e.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedEntity;

import java.util.List;

/**
 * Re-schedules all active medicine alarms after a device reboot.
 * AlarmManager alarms are lost on reboot, so this receiver
 * queries the database and sets them all up again.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            Log.d(TAG, "Boot completed — rescheduling all medicine alarms");

            // Database access must be off the main thread
            new Thread(() -> {
                try {
                    List<MedEntity> meds = AppDataBase.getInstance(context)
                            .medDAO()
                            .getActiveMeds();

                    for (MedEntity med : meds) {
                        ReminderScheduler.schedule(context, med);
                        Log.d(TAG, "Rescheduled: " + med.name + " at " + med.time);
                    }

                    Log.d(TAG, "Rescheduled " + meds.size() + " medicine alarm(s)");
                } catch (Exception e) {
                    Log.e(TAG, "Error rescheduling alarms after boot", e);
                }
            }).start();
        }
    }
}
