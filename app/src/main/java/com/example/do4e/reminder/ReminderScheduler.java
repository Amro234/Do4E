package com.example.do4e.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;

public class ReminderScheduler {

    public static void schedule(Context context, String medName, String time, int notifId) {
        // Parse "HH:MM AM/PM"
        int hour, minute;
        try {
            String[] parts = time.split(":");
            hour = Integer.parseInt(parts[0].trim());
            String[] minAmPm = parts[1].trim().split(" ");
            minute = Integer.parseInt(minAmPm[0]);
            String amPm = minAmPm[1];

            // convert to 24-hour for calendar
            if (amPm.equalsIgnoreCase("PM") && hour != 12)
                hour += 12;
            if (amPm.equalsIgnoreCase("AM") && hour == 12)
                hour = 0;

        } catch (Exception e) {
            e.printStackTrace();
            return; // if format is wrong, don't schedule
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // if time passed today → schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("med_name", medName);
        intent.putExtra("notif_id", notifId);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        setAlarm(alarmManager, calendar.getTimeInMillis(), pendingIntent);
    }

    // called from ReminderReceiver after notification appears
    public static void rescheduleNextDay(Context context, Intent oldIntent) {
        String medName = oldIntent.getStringExtra("med_name");
        int notifId = oldIntent.getIntExtra("notif_id", 0);
        int hour = oldIntent.getIntExtra("hour", 0);
        int minute = oldIntent.getIntExtra("minute", 0);

        // here we build the time string again to pass it to schedule()
        // but schedule() expects AM/PM — pass 24h directly instead
        scheduleFrom24h(context, medName, hour, minute, notifId);
    }

    // internal helper that takes 24h directly (used from rescheduleNextDay only)
    private static void scheduleFrom24h(Context context, String medName,
            int hour, int minute, int notifId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 1); // always tomorrow

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("med_name", medName);
        intent.putExtra("notif_id", notifId);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        setAlarm(alarmManager, calendar.getTimeInMillis(), pendingIntent);
    }

    private static void setAlarm(AlarmManager alarmManager, long timeInMillis, PendingIntent pendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void cancelAlarm(Context context, String medName, String time) {
        int notifId = (medName + time).hashCode();

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }
}