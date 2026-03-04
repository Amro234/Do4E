package com.example.do4e.ui.settings;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do4e.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

/**
 * SettingsFragment
 *
 * Contains three sections:
 * 1. Audio Type selector — "Music" vs "Voice"
 * 2. Track / Voice picker — shows the relevant sub-list based on the type
 * 3. Snooze duration — NumberPicker (or Slider) to choose 5 / 10 / 15 / 20 / 30
 * min
 *
 * All choices are persisted in SharedPreferences ("do4e_settings") so that
 * ReminderReceiver and med_alarm can read them at notification time.
 *
 * SharedPreferences keys (use SettingsPrefs constants from any class):
 * KEY_AUDIO_TYPE → "music" | "voice"
 * KEY_MUSIC_TRACK → resource-name string of the chosen music file
 * KEY_VOICE_TRACK → resource-name string of the chosen voice file
 * KEY_SNOOZE_MINUTES → int (5 / 10 / 15 / 20 / 30)
 */
public class settings extends Fragment {

    // ── SharedPreferences ─────────────────────────────────────────────────
    public static final String PREFS_NAME = "do4e_settings";
    public static final String KEY_AUDIO_TYPE = "audio_type";
    public static final String KEY_MUSIC_TRACK = "music_track";
    public static final String KEY_VOICE_TRACK = "voice_track";
    public static final String KEY_SNOOZE_MINUTES = "snooze_minutes";

    public static final String AUDIO_TYPE_MUSIC = "music";
    public static final String AUDIO_TYPE_VOICE = "voice";

    // ── Default snooze ────────────────────────────────────────────────────
    public static final int DEFAULT_SNOOZE_MINUTES = 10;

    // ── Music tracks (add your raw/ resource names here) ────────────────
    // Each entry = { displayName, rawResourceName }
    // Make sure you add matching files under res/raw/ in your project.
    private static final String[][] MUSIC_TRACKS = {
            { "blinke", "notification_sound_01" },
            { "Winkee", "notification_sound_02" },
            { "zinkee", "notification_sound_03" },
            { "ringee", "notification_sound_04" }
    };

    // ── Voice tracks (add your raw/ resource names here) ────────────────
    private static final String[][] VOICE_TRACKS = {
            { "ArabicFemale", "voice_female_02" },
            { "ArabicMale", "voice_male_02" },
            { "EnglishFemale", "voice_female_01" },
            { "EnglishMale", "voice_male_01" },
    };

    // ── Snooze options (minutes) ──────────────────────────────────────────
    private static final int[] SNOOZE_OPTIONS = { 5, 10, 15, 20, 30 };

    // ── Views ─────────────────────────────────────────────────────────────
    private RadioGroup rgAudioType;
    private RadioButton rbMusic, rbVoice;
    private LinearLayout llMusicTracks, llVoiceTracks;
    private RadioGroup rgMusicTracks, rgVoiceTracks;
    private RadioGroup rgSnooze;
    private ImageButton ibPreviewStop;

    // ── State ─────────────────────────────────────────────────────────────
    private MediaPlayer mediaPlayer;
    private int previewingResId = -1;

    // ─────────────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Wire top-level views ──────────────────────────────────────────
        rgAudioType = view.findViewById(R.id.rg_audio_type);
        rbMusic = view.findViewById(R.id.rb_audio_music);
        rbVoice = view.findViewById(R.id.rb_audio_voice);
        llMusicTracks = view.findViewById(R.id.ll_music_tracks);
        llVoiceTracks = view.findViewById(R.id.ll_voice_tracks);
        rgMusicTracks = view.findViewById(R.id.rg_music_tracks);
        rgVoiceTracks = view.findViewById(R.id.rg_voice_tracks);
        rgSnooze = view.findViewById(R.id.rg_snooze);
        ibPreviewStop = view.findViewById(R.id.ib_preview_stop);

        // ── Populate dynamic track lists ──────────────────────────────────
        populateMusicTracks();
        populateVoiceTracks();
        populateSnoozeOptions();

        // ── Restore saved preferences ─────────────────────────────────────
        restorePreferences();

        // ── Listeners ─────────────────────────────────────────────────────
        rgAudioType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_audio_music) {
                llMusicTracks.setVisibility(View.VISIBLE);
                llVoiceTracks.setVisibility(View.GONE);
                saveString(KEY_AUDIO_TYPE, AUDIO_TYPE_MUSIC);
            } else {
                llMusicTracks.setVisibility(View.GONE);
                llVoiceTracks.setVisibility(View.VISIBLE);
                saveString(KEY_AUDIO_TYPE, AUDIO_TYPE_VOICE);
            }
            stopPreview();
        });

        if (ibPreviewStop != null) {
            ibPreviewStop.setOnClickListener(v -> stopPreview());
        }

        // Save button
        MaterialButton btnSave = view.findViewById(R.id.btn_settings_save);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                saveAllSettings();
                Toast.makeText(requireContext(),
                        "Settings saved!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Populate helpers
    // ─────────────────────────────────────────────────────────────────────

    private void populateMusicTracks() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < MUSIC_TRACKS.length; i++) {
            String displayName = MUSIC_TRACKS[i][0];
            String resourceName = MUSIC_TRACKS[i][1];

            View row = inflater.inflate(R.layout.item_audio_track, rgMusicTracks, false);
            RadioButton rb = row.findViewById(R.id.rb_track);
            TextView tv = row.findViewById(R.id.tv_track_name);
            ImageButton ib = row.findViewById(R.id.ib_track_preview);

            rb.setId(View.generateViewId());
            rb.setTag(resourceName);
            tv.setText(displayName);

            ib.setOnClickListener(v -> previewTrack(resourceName));
            rb.setOnCheckedChangeListener((btn, checked) -> {
                if (checked)
                    saveString(KEY_MUSIC_TRACK, resourceName);
            });

            rgMusicTracks.addView(row);
        }
    }

    private void populateVoiceTracks() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < VOICE_TRACKS.length; i++) {
            String displayName = VOICE_TRACKS[i][0];
            String resourceName = VOICE_TRACKS[i][1];

            View row = inflater.inflate(R.layout.item_audio_track, rgVoiceTracks, false);
            RadioButton rb = row.findViewById(R.id.rb_track);
            TextView tv = row.findViewById(R.id.tv_track_name);
            ImageButton ib = row.findViewById(R.id.ib_track_preview);

            rb.setId(View.generateViewId());
            rb.setTag(resourceName);
            tv.setText(displayName);

            ib.setOnClickListener(v -> previewTrack(resourceName));
            rb.setOnCheckedChangeListener((btn, checked) -> {
                if (checked)
                    saveString(KEY_VOICE_TRACK, resourceName);
            });

            rgVoiceTracks.addView(row);
        }
    }

    private void populateSnoozeOptions() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int minutes : SNOOZE_OPTIONS) {
            RadioButton rb = (RadioButton) inflater.inflate(
                    R.layout.item_snooze_option, rgSnooze, false);
            rb.setId(View.generateViewId());
            rb.setText(minutes + " min");
            rb.setTag(minutes);
            rb.setOnCheckedChangeListener((btn, checked) -> {
                if (checked)
                    saveInt(KEY_SNOOZE_MINUTES, minutes);
            });
            rgSnooze.addView(rb);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Preferences
    // ─────────────────────────────────────────────────────────────────────

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences(PREFS_NAME, 0);
    }

    private void saveString(String key, String value) {
        prefs().edit().putString(key, value).apply();
    }

    private void saveInt(String key, int value) {
        prefs().edit().putInt(key, value).apply();
    }

    /** Restore every setting from prefs and update the UI to match. */
    private void restorePreferences() {
        SharedPreferences sp = prefs();

        // Audio type
        String audioType = sp.getString(KEY_AUDIO_TYPE, AUDIO_TYPE_MUSIC);
        if (AUDIO_TYPE_VOICE.equals(audioType)) {
            rbVoice.setChecked(true);
            llMusicTracks.setVisibility(View.GONE);
            llVoiceTracks.setVisibility(View.VISIBLE);
        } else {
            rbMusic.setChecked(true);
            llMusicTracks.setVisibility(View.VISIBLE);
            llVoiceTracks.setVisibility(View.GONE);
        }

        // Selected music track
        String savedMusic = sp.getString(KEY_MUSIC_TRACK, "");
        selectTrackInGroup(rgMusicTracks, savedMusic);

        // Selected voice track
        String savedVoice = sp.getString(KEY_VOICE_TRACK, "");
        selectTrackInGroup(rgVoiceTracks, savedVoice);

        // Snooze
        int savedSnooze = sp.getInt(KEY_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES);
        selectSnoozeInGroup(savedSnooze);
    }

    private void selectTrackInGroup(RadioGroup group, String resourceName) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            // Each child is the inflated row; rb is inside it
            RadioButton rb = child.findViewById(R.id.rb_track);
            if (rb != null && resourceName.equals(rb.getTag())) {
                rb.setChecked(true);
                return;
            }
        }
        // Nothing matched — check first row by default
        if (group.getChildCount() > 0) {
            RadioButton first = group.getChildAt(0).findViewById(R.id.rb_track);
            if (first != null)
                first.setChecked(true);
        }
    }

    private void selectSnoozeInGroup(int minutes) {
        for (int i = 0; i < rgSnooze.getChildCount(); i++) {
            RadioButton rb = (RadioButton) rgSnooze.getChildAt(i);
            if (rb.getTag() != null && (int) rb.getTag() == minutes) {
                rb.setChecked(true);
                return;
            }
        }
        // Default: 10 min
        if (rgSnooze.getChildCount() > 1)
            ((RadioButton) rgSnooze.getChildAt(1)).setChecked(true);
    }

    /** Persist everything at once (called by Save button). */
    private void saveAllSettings() {
        // Audio type is already saved live; this is a safety flush
        SharedPreferences.Editor editor = prefs().edit();

        // Audio type
        editor.putString(KEY_AUDIO_TYPE,
                rbVoice.isChecked() ? AUDIO_TYPE_VOICE : AUDIO_TYPE_MUSIC);

        // Music track
        String musicTrack = getCheckedTrackTag(rgMusicTracks);
        if (musicTrack != null)
            editor.putString(KEY_MUSIC_TRACK, musicTrack);

        // Voice track
        String voiceTrack = getCheckedTrackTag(rgVoiceTracks);
        if (voiceTrack != null)
            editor.putString(KEY_VOICE_TRACK, voiceTrack);

        // Snooze
        for (int i = 0; i < rgSnooze.getChildCount(); i++) {
            RadioButton rb = (RadioButton) rgSnooze.getChildAt(i);
            if (rb.isChecked() && rb.getTag() != null) {
                editor.putInt(KEY_SNOOZE_MINUTES, (int) rb.getTag());
                break;
            }
        }

        editor.apply();
    }

    @Nullable
    private String getCheckedTrackTag(RadioGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            RadioButton rb = group.getChildAt(i).findViewById(R.id.rb_track);
            if (rb != null && rb.isChecked())
                return (String) rb.getTag();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Audio preview
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Plays a preview of the chosen track.
     * 
     * @param resourceName The name of the raw resource (without extension).
     */
    private void previewTrack(String resourceName) {
        stopPreview();

        int resId = requireContext().getResources().getIdentifier(
                resourceName, "raw", requireContext().getPackageName());

        if (resId == 0) {
            Toast.makeText(requireContext(),
                    "Audio file not found: " + resourceName, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mediaPlayer = MediaPlayer.create(requireContext(), resId);
            if (mediaPlayer != null) {
                previewingResId = resId;
                if (ibPreviewStop != null)
                    ibPreviewStop.setVisibility(View.VISIBLE);
                mediaPlayer.setOnCompletionListener(mp -> stopPreview());
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Could not play preview", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPreview() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying())
                    mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
        previewingResId = -1;
        if (ibPreviewStop != null)
            ibPreviewStop.setVisibility(View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle — release player on leave
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onPause() {
        super.onPause();
        stopPreview();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPreview();
    }
}