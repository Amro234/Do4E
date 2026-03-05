package com.example.do4e.core.utility;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.view.View;
import com.example.do4e.R;

/**
 * ClickSoundHelper
 *
 * A singleton utility that plays a short click sound (res/raw/clickaudio.wav)
 * whenever a clickable view is tapped.
 */
public class ClickSoundHelper {

    private static ClickSoundHelper instance;

    private final SoundPool soundPool;
    private int clickSoundId = -1;
    private boolean loaded = false;

    private ClickSoundHelper(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();

        clickSoundId = soundPool.load(context.getApplicationContext(), R.raw.clickaudio, 1);
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0)
                loaded = true;
        });
    }

    /** Get (or create) the singleton instance. */
    public static synchronized ClickSoundHelper get(Context context) {
        if (instance == null) {
            instance = new ClickSoundHelper(context.getApplicationContext());
        }
        return instance;
    }

    /** Play the click sound once. */
    public void playClick() {
        if (loaded && clickSoundId != -1) {
            soundPool.play(clickSoundId, 0.5f, 0.5f, 1, 0, 1.0f);
        }
    }

    /**
     * Recursively walks the view-tree and attaches the click-sound wrapper
     * to every view that is clickable.
     */
    public void applyToAllClickable(View root) {
        if (root == null)
            return;
        if (root.isClickable()) {
            root.setSoundEffectsEnabled(false);
            root.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    playClick();
                }
                return false; // let the normal click still fire
            });
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyToAllClickable(group.getChildAt(i));
            }
        }
    }

    /**
     * Convenience: wrap a click listener so it plays the click sound first.
     */
    public View.OnClickListener wrap(View.OnClickListener listener) {
        return v -> {
            playClick();
            if (listener != null)
                listener.onClick(v);
        };
    }
}
