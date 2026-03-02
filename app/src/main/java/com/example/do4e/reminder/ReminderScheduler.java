package com.example.do4e.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;

import com.example.do4e.db.MedEntity;

public class ReminderScheduler {

    public static void schedule(Context context, MedEntity med) {
        int notifId = (med.name + med.time).hashCode();

        Calendar calendar = Calendar.getInstance();
        if (med.startDate != 0) {
            // MaterialDatePicker returns midnight UTC.
            // We want to keep the selected date but apply local time.
            Calendar startCal = Calendar.getInstance();
            startCal.setTimeInMillis(med.startDate);

            calendar.set(Calendar.YEAR, startCal.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, startCal.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, startCal.get(Calendar.DAY_OF_MONTH));
        }

        calendar.set(Calendar.HOUR_OF_DAY, med.hour);
        calendar.set(Calendar.MINUTE, med.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the resulting time is in the past, move it forward
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("med_name", med.name);
        intent.putExtra("notif_id", notifId);
        intent.putExtra("med_id", med.id_meds);
        intent.putExtra("hour", med.hour);
        intent.putExtra("minute", med.minute);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        setAlarm(alarmManager, calendar.getTimeInMillis(), pendingIntent);
    }

    public static void schedule(Context context, String medName, String time, int notifId, int medId) {
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
        intent.putExtra("med_id", medId);
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

        int medId = oldIntent.getIntExtra("med_id", -1);

        // here we build the time string again to pass it to schedule()
        // but schedule() expects AM/PM — pass 24h directly instead
        scheduleFrom24h(context, medName, hour, minute, notifId, medId);
    }

    // internal helper that takes 24h directly (used from rescheduleNextDay only)
    private static void scheduleFrom24h(Context context, String medName,
            int hour, int minute, int notifId, int medId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, 1); // always tomorrow

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("med_name", medName);
        intent.putExtra("notif_id", notifId);
        intent.putExtra("med_id", medId);
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