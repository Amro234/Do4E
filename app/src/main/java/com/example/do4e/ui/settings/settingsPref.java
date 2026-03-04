package com.example.do4e.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * SettingsPrefs
 *
 * A static helper that any class (ReminderReceiver, med_alarm, etc.) can call
 * to read the user's Audio Manager and Snooze settings without needing a
 * reference to the Settings fragment itself.
 *
 * Usage example in med_alarm.java:
 *
 *   // Play the configured alarm sound
 *   SettingsPrefs.playAlarmSound(this, player -> this.alarmPlayer = player);
 *
 *   // Get snooze duration
 *   int snoozeMinutes = SettingsPrefs.getSnoozeMinutes(this);
 */
public class settingsPref {

    private static final String TAG = "SettingsPrefs";

    // ── Read helpers ──────────────────────────────────────────────────────

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(settings.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Returns "music" or "voice". Default = "music". */
    public static String getAudioType(Context context) {
        return getPrefs(context).getString(
                settings.KEY_AUDIO_TYPE, settings.AUDIO_TYPE_MUSIC);
    }

    /**
     * Returns the raw resource name for the chosen music track.
     * Default = first entry ("alarm_bells").
     */
    public static String getMusicTrack(Context context) {
        return getPrefs(context).getString(settings.KEY_MUSIC_TRACK, "alarm_bells");
    }

    /**
     * Returns the raw resource name for the chosen voice track.
     * Default = first entry ("voice_time_to_take").
     */
    public static String getVoiceTrack(Context context) {
        return getPrefs(context).getString(settings.KEY_VOICE_TRACK, "voice_time_to_take");
    }

    /**
     * Returns the active track resource name based on the selected audio type.
     * This is the single method ReminderReceiver / med_alarm should call.
     */
    public static String getActiveTrackName(Context context) {
        if (settings.AUDIO_TYPE_VOICE.equals(getAudioType(context))) {
            return getVoiceTrack(context);
        }
        return getMusicTrack(context);
    }

    /** Returns the snooze duration in minutes. Default = 10. */
    public static int getSnoozeMinutes(Context context) {
        return getPrefs(context).getInt(
                settings.KEY_SNOOZE_MINUTES, settings.DEFAULT_SNOOZE_MINUTES);
    }

    // ── Playback helper ───────────────────────────────────────────────────

    /**
     * Creates and starts a MediaPlayer for the user's chosen alarm sound.
     *
     * @param context  Any context (Activity, Service, BroadcastReceiver via
     *                 context.getApplicationContext()).
     * @param callback Called on the calling thread with the started player so
     *                 the caller can hold a reference and call release() later.
     *                 Callback receives null if the resource was not found.
     */
    public static void playAlarmSound(Context context, PlayerCallback callback) {
        String trackName = getActiveTrackName(context);
        int resId = context.getResources().getIdentifier(
                trackName, "raw", context.getPackageName());

        if (resId == 0) {
            Log.w(TAG, "Audio resource not found: " + trackName);
            if (callback != null) callback.onPlayerReady(null);
            return;
        }

        try {
            MediaPlayer player = MediaPlayer.create(context, resId);
            if (player != null) {
                player.setLooping(false);
                player.start();
                if (callback != null) callback.onPlayerReady(player);
            } else {
                if (callback != null) callback.onPlayerReady(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play alarm sound", e);
            if (callback != null) callback.onPlayerReady(null);
        }
    }

    /** Callback interface for playAlarmSound(). */
    public interface PlayerCallback {
        void onPlayerReady(MediaPlayer player);
    }
}