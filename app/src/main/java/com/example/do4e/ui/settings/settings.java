package com.example.do4e.ui.settings;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do4e.core.utility.ClickSoundHelper;
import com.example.do4e.R;
import com.google.android.material.button.MaterialButton;

/**
 * SettingsFragment
 *
 * Contains three sections:
 * 1. Audio Type selector — "Ringtone" vs "Voice Message"
 * 2. Track / Voice picker — Spinner dropdowns that auto-play on selection
 * 3. Snooze duration — RadioButtons to choose 5 / 10 / 15 / 20 / 30 min
 *
 * All choices are persisted in SharedPreferences ("do4e_settings") so that
 * ReminderReceiver and med_alarm can read them at notification time.
 */
public class settings extends Fragment {

    // ── SharedPreferences ─────────────────────────────────────────────────
    public static final String PREFS_NAME = "do4e_settings";
    public static final String KEY_AUDIO_MUSIC_ENABLED = "audio_music_enabled";
    public static final String KEY_AUDIO_VOICE_ENABLED = "audio_voice_enabled";
    public static final String KEY_AUDIO_TYPE = "audio_type"; // For migration
    public static final String KEY_MUSIC_TRACK = "music_track";
    public static final String KEY_VOICE_TRACK = "voice_track";
    public static final String KEY_SNOOZE_MINUTES = "snooze_minutes";

    // ── Default snooze ────────────────────────────────────────────────────
    public static final int DEFAULT_SNOOZE_MINUTES = 10;

    // ── Music tracks (add your raw/ resource names here) ────────────────
    // Each entry = { displayName, rawResourceName }
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
    private CheckBox cbMusic, cbVoice;
    private LinearLayout llMusicTracks, llVoiceTracks;
    private AutoCompleteTextView spinnerMusic, spinnerVoice, spinnerSnooze;

    // ── State ─────────────────────────────────────────────────────────────
    private MediaPlayer mediaPlayer;

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
        cbMusic = view.findViewById(R.id.cb_audio_music);
        cbVoice = view.findViewById(R.id.cb_audio_voice);
        llMusicTracks = view.findViewById(R.id.ll_music_tracks);
        llVoiceTracks = view.findViewById(R.id.ll_voice_tracks);
        spinnerMusic = view.findViewById(R.id.spinner_music_tracks);
        spinnerVoice = view.findViewById(R.id.spinner_voice_tracks);
        spinnerSnooze = view.findViewById(R.id.spinner_snooze);

        // ── Populate Spinners and Snooze ──────────────────────────────────
        populateMusicSpinner();
        populateVoiceSpinner();
        populateSnoozeOptions();

        // ── Restore saved preferences ─────────────────────────────────────
        restorePreferences();

        // ── Audio-type toggle listener ────────────────────────────────────
        cbMusic.setOnCheckedChangeListener((btn, isChecked) -> {
            if (btn.isPressed())
                ClickSoundHelper.get(requireContext()).playClick();
            if (!isChecked && !cbVoice.isChecked()) {
                cbMusic.setChecked(true); // Don't allow unchecking both
                Toast.makeText(requireContext(), "Must select at least one audio type", Toast.LENGTH_SHORT).show();
            } else {
                updateAudioVisibility();
                stopPreview();
            }
        });
        cbVoice.setOnCheckedChangeListener((btn, isChecked) -> {
            if (btn.isPressed())
                ClickSoundHelper.get(requireContext()).playClick();
            if (!isChecked && !cbMusic.isChecked()) {
                cbVoice.setChecked(true); // Don't allow unchecking both
                Toast.makeText(requireContext(), "Must select at least one audio type", Toast.LENGTH_SHORT).show();
            } else {
                updateAudioVisibility();
                stopPreview();
            }
        });

        // ── Save button ───────────────────────────────────────────────────
        MaterialButton btnSave = view.findViewById(R.id.btn_settings_save);
        if (btnSave != null) {
            btnSave.setOnClickListener(
                    ClickSoundHelper.get(requireContext()).wrap(v -> {
                        saveAllSettings();
                        Toast.makeText(requireContext(),
                                "Settings saved!", Toast.LENGTH_SHORT).show();
                    }));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Populate helpers
    // ─────────────────────────────────────────────────────────────────────

    private void updateAudioVisibility() {
        llMusicTracks.setVisibility(cbMusic.isChecked() ? View.VISIBLE : View.GONE);
        llVoiceTracks.setVisibility(cbVoice.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void populateMusicSpinner() {
        String[] displayNames = new String[MUSIC_TRACKS.length];
        for (int i = 0; i < MUSIC_TRACKS.length; i++) {
            displayNames[i] = MUSIC_TRACKS[i][0];
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayNames) {
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults filterResults = new FilterResults();
                        filterResults.values = displayNames;
                        filterResults.count = displayNames.length;
                        return filterResults;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };

        spinnerMusic.setAdapter(adapter);

        spinnerMusic.setOnItemClickListener((parent, view, position, id) -> {
            String resourceName = MUSIC_TRACKS[position][1];
            saveString(KEY_MUSIC_TRACK, resourceName);
            previewTrack(resourceName);
            ClickSoundHelper.get(requireContext()).playClick();
        });
    }

    private void populateVoiceSpinner() {
        String[] displayNames = new String[VOICE_TRACKS.length];
        for (int i = 0; i < VOICE_TRACKS.length; i++) {
            displayNames[i] = VOICE_TRACKS[i][0];
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                displayNames) {
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults filterResults = new FilterResults();
                        filterResults.values = displayNames;
                        filterResults.count = displayNames.length;
                        return filterResults;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };

        spinnerVoice.setAdapter(adapter);

        spinnerVoice.setOnItemClickListener((parent, view, position, id) -> {
            String resourceName = VOICE_TRACKS[position][1];
            saveString(KEY_VOICE_TRACK, resourceName);
            previewTrack(resourceName);
            ClickSoundHelper.get(requireContext()).playClick();
        });
    }

    private void populateSnoozeOptions() {
        String[] snoozeStrings = new String[SNOOZE_OPTIONS.length];
        for (int i = 0; i < SNOOZE_OPTIONS.length; i++) {
            snoozeStrings[i] = SNOOZE_OPTIONS[i] + " min";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                snoozeStrings) {
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults filterResults = new FilterResults();
                        filterResults.values = snoozeStrings;
                        filterResults.count = snoozeStrings.length;
                        return filterResults;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };

        spinnerSnooze.setAdapter(adapter);

        spinnerSnooze.setOnItemClickListener((parent, view, position, id) -> {
            ClickSoundHelper.get(requireContext()).playClick();
        });
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

        // Audio type migration / load
        boolean hasMusic = sp.getBoolean(KEY_AUDIO_MUSIC_ENABLED, true);
        boolean hasVoice = sp.getBoolean(KEY_AUDIO_VOICE_ENABLED, false);

        if (sp.contains(KEY_AUDIO_TYPE)) {
            String oldType = sp.getString(KEY_AUDIO_TYPE, "music");
            hasMusic = "music".equals(oldType);
            hasVoice = "voice".equals(oldType);
            sp.edit().remove(KEY_AUDIO_TYPE)
                    .putBoolean(KEY_AUDIO_MUSIC_ENABLED, hasMusic)
                    .putBoolean(KEY_AUDIO_VOICE_ENABLED, hasVoice).apply();
        }

        cbMusic.setChecked(hasMusic);
        cbVoice.setChecked(hasVoice);
        updateAudioVisibility();

        // Selected music track → set spinner position
        String savedMusic = sp.getString(KEY_MUSIC_TRACK, "");
        selectSpinnerByResource(spinnerMusic, MUSIC_TRACKS, savedMusic);

        // Selected voice track → set spinner position
        String savedVoice = sp.getString(KEY_VOICE_TRACK, "");
        selectSpinnerByResource(spinnerVoice, VOICE_TRACKS, savedVoice);

        // Snooze
        int savedSnooze = sp.getInt(KEY_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES);
        selectSnoozeInSpinner(savedSnooze);
    }

    private void selectSpinnerByResource(AutoCompleteTextView spinner, String[][] tracks, String resourceName) {
        for (int i = 0; i < tracks.length; i++) {
            if (tracks[i][1].equals(resourceName)) {
                spinner.setText(tracks[i][0], false);
                return;
            }
        }
        // Default: first item
        spinner.setText(tracks[0][0], false);
    }

    private void selectSnoozeInSpinner(int minutes) {
        spinnerSnooze.setText(minutes + " min", false);
    }

    /** Persist everything at once (called by Save button). */
    private void saveAllSettings() {
        SharedPreferences.Editor editor = prefs().edit();

        // Audio type
        editor.putBoolean(KEY_AUDIO_MUSIC_ENABLED, cbMusic.isChecked());
        editor.putBoolean(KEY_AUDIO_VOICE_ENABLED, cbVoice.isChecked());

        // Music track
        String currentMusicText = spinnerMusic.getText().toString();
        for (String[] track : MUSIC_TRACKS) {
            if (track[0].equals(currentMusicText)) {
                editor.putString(KEY_MUSIC_TRACK, track[1]);
                break;
            }
        }

        // Voice track
        String currentVoiceText = spinnerVoice.getText().toString();
        for (String[] track : VOICE_TRACKS) {
            if (track[0].equals(currentVoiceText)) {
                editor.putString(KEY_VOICE_TRACK, track[1]);
                break;
            }
        }

        // Snooze
        String snoozeText = spinnerSnooze.getText().toString();
        if (snoozeText.contains(" min")) {
            try {
                int minutes = Integer.parseInt(snoozeText.replace(" min", ""));
                editor.putInt(KEY_SNOOZE_MINUTES, minutes);
            } catch (Exception ignored) {
            }
        }

        editor.apply();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Audio preview
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Plays a preview of the chosen track.
     * Automatically runs when the user selects from the dropdown.
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