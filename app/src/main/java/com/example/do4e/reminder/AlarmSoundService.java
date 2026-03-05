package com.example.do4e.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.do4e.R;
import com.example.do4e.ui.notification.med_alarm;
import com.example.do4e.ui.settings.settingsPref;

public class AlarmSoundService extends Service {

    public static final String ACTION_STOP = "com.example.do4e.action.STOP_ALARM";
    private static final String CHANNEL_ID = "med_reminder_silent_channel_v2";

    private MediaPlayer alarmPlayer;
    private Handler voiceHandler = new Handler(Looper.getMainLooper());
    private boolean isDismissed = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String medName = intent.getStringExtra("med_name");
        int notifId = intent.getIntExtra("notif_id", 0);
        int medId = intent.getIntExtra("med_id", -1);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // ── Create Custom Silent Channel ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medication Alarms",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Silent channel for custom alarm sounds");
            channel.enableVibration(true);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);
        }

        // ── Intents ──
        Intent takenIntent = new Intent(this, MedicineActionReceiver.class);
        takenIntent.putExtra("med_id", medId);
        takenIntent.putExtra("notif_id", notifId);
        // We add an extra to let MedicineActionReceiver know to stop the service
        takenIntent.putExtra("stop_service", true);
        PendingIntent takenPendingIntent = PendingIntent.getBroadcast(
                this,
                notifId,
                takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent fullScreenIntent = new Intent(this, med_alarm.class);
        fullScreenIntent.putExtra("med_id", medId);
        fullScreenIntent.putExtra("notif_id", notifId);
        fullScreenIntent.putExtra("med_name", medName);
        fullScreenIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                notifId + 10000,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ── Build Notification ──
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.pill_24dp_icon)
                .setContentTitle("Medicine Time!")
                .setContentText("Time to take " + medName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .setSound(null)
                .setAutoCancel(true)
                .setContentIntent(fullScreenPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .addAction(R.drawable.check_24dp_icon, "Mark as Taken", takenPendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notifId, builder.build(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(notifId, builder.build());
        }

        // ── Play Audio ──
        playAudioLogic();

        return START_STICKY;
    }

    private void playAudioLogic() {
        boolean hasMusic = settingsPref.hasMusic(this);
        boolean hasVoice = settingsPref.hasVoice(this);

        if (hasMusic && hasVoice) {
            settingsPref.playSound(this, settingsPref.getMusicTrack(this), player -> {
                alarmPlayer = player;
                voiceHandler.postDelayed(() -> {
                    if (isDismissed)
                        return;
                    settingsPref.playSound(this, settingsPref.getVoiceTrack(this), voicePlayer -> {
                        if (isDismissed) {
                            if (voicePlayer != null)
                                voicePlayer.stop();
                        } else {
                            if (alarmPlayer != null && alarmPlayer.isPlaying())
                                alarmPlayer.stop();
                            alarmPlayer = voicePlayer;
                        }
                    });
                }, 1000);
            });
        } else if (hasVoice) {
            settingsPref.playSound(this, settingsPref.getVoiceTrack(this), player -> alarmPlayer = player);
        } else {
            settingsPref.playSound(this, settingsPref.getMusicTrack(this), player -> alarmPlayer = player);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDismissed = true;
        voiceHandler.removeCallbacksAndMessages(null);
        if (alarmPlayer != null) {
            try {
                if (alarmPlayer.isPlaying())
                    alarmPlayer.stop();
                alarmPlayer.release();
            } catch (Exception ignored) {
            }
            alarmPlayer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
