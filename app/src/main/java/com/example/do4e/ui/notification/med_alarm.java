package com.example.do4e.ui.notification;

import android.app.NotificationManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.do4e.core.utility.ClickSoundHelper;
import com.example.do4e.R;
import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.ReminderScheduler;

import com.example.do4e.reminder.AlarmSoundService;
import com.example.do4e.ui.settings.settingsPref;

import java.util.Calendar;

public class med_alarm extends AppCompatActivity {

    private int medId = -1;
    private int notifId = -1;
    private MedEntity med;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Show over the lock screen & wake the display ──────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null)
                km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.activity_med_alarm);

        // ── Block back gesture / button (user must tap a button) ──────────
        // Replaces the deprecated onBackPressed() override.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Intentionally empty — do nothing on back
            }
        });

        // ── Read extras passed by ReminderReceiver ────────────────────────
        medId = getIntent().getIntExtra("med_id", -1);
        notifId = getIntent().getIntExtra("notif_id", 0);

        // ── Load medicine from DB then populate UI ────────────────────────
        loadMedicineAndPopulateUI();

        // ── Button wiring ─────────────────────────────────────────────────
        findViewById(R.id.btn_log_taken).setOnClickListener(ClickSoundHelper.get(this).wrap(v -> handleLogTaken()));
        findViewById(R.id.btn_snooze).setOnClickListener(ClickSoundHelper.get(this).wrap(v -> handleSnooze()));
        findViewById(R.id.btn_details).setOnClickListener(ClickSoundHelper.get(this).wrap(v -> handleDetails()));
        findViewById(R.id.btn_skip).setOnClickListener(ClickSoundHelper.get(this).wrap(v -> handleSkip()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data loading
    // ─────────────────────────────────────────────────────────────────────────

    private void loadMedicineAndPopulateUI() {
        if (medId == -1)
            return;

        AppDataBase db = AppDataBase.getInstance(this);
        new Thread(() -> {
            med = db.medDAO().getById(medId);
            runOnUiThread(() -> {
                if (med == null)
                    return;

                // Medicine name
                ((TextView) findViewById(R.id.tv_alarm_med_name)).setText(med.name);

                // Subtitle label
                TextView tvArabic = findViewById(R.id.tv_alarm_med_name_arabic);
                if (tvArabic != null)
                    tvArabic.setText(med.name);

                // Dosage
                TextView tvDosage = findViewById(R.id.tv_alarm_dosage);
                if (tvDosage != null)
                    tvDosage.setText(med.dosage != null && !med.dosage.isEmpty()
                            ? med.dosage
                            : "—");

                // Time
                TextView tvTime = findViewById(R.id.tv_alarm_time);
                if (tvTime != null)
                    tvTime.setText(med.time);

                // Instruction
                TextView tvInstruction = findViewById(R.id.tv_alarm_instruction);
                if (tvInstruction != null && med.instruction != null)
                    tvInstruction.setText(med.instruction);

                // Med type icon (top) - static icon
                ImageView ivIcon = findViewById(R.id.iv_alarm_med_icon);
                if (ivIcon != null) {
                    ivIcon.setImageResource(R.drawable.live_reminder_icon);
                }

                // Med photo (second image) - swapping based on type
                ImageView ivPhoto = findViewById(R.id.iv_alarm_med_photo);
                if (ivPhoto != null) {
                    if ("Syrup".equalsIgnoreCase(med.medType))
                        ivPhoto.setImageResource(R.drawable.syrup1);
                    else if ("Syringe".equalsIgnoreCase(med.medType))
                        ivPhoto.setImageResource(R.drawable.syringes2);
                    else
                        ivPhoto.setImageResource(R.drawable.pill1);
                }

                // Instruction icon - swapping based on text
                ImageView ivInstructionIcon = findViewById(R.id.iv_alarm_instruction_icon);
                if (ivInstructionIcon != null) {
                    if ("Before Food".equalsIgnoreCase(med.instruction))
                        ivInstructionIcon.setImageResource(R.drawable.before_food_24dp_icon);
                    else if ("During Food".equalsIgnoreCase(med.instruction))
                        ivInstructionIcon.setImageResource(R.drawable.during_food_24dp_icon);
                    else if ("After Food".equalsIgnoreCase(med.instruction))
                        ivInstructionIcon.setImageResource(R.drawable.after_food_24dp_icon);
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button handlers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleLogTaken() {
        if (medId == -1) {
            finishAndDismiss();
            return;
        }
        AppDataBase db = AppDataBase.getInstance(this);
        new Thread(() -> {
            db.medDAO().incrementDaysTaken(medId);
            runOnUiThread(this::finishAndDismiss);
        }).start();
    }

    private void handleSnooze() {
        if (med == null) {
            finishAndDismiss();
            return;
        }

        // ✅ Read snooze duration from Settings instead of hardcoded 10
        int snoozeMinutes = settingsPref.getSnoozeMinutes(this);

        Calendar snooze = Calendar.getInstance();
        snooze.add(Calendar.MINUTE, snoozeMinutes);

        MedEntity snoozeMed = new MedEntity();
        snoozeMed.id_meds = med.id_meds;
        snoozeMed.name = med.name;
        snoozeMed.time = med.time;
        snoozeMed.hour = snooze.get(Calendar.HOUR_OF_DAY);
        snoozeMed.minute = snooze.get(Calendar.MINUTE);
        snoozeMed.startDate = snooze.getTimeInMillis();

        ReminderScheduler.schedule(this, snoozeMed);
        finishAndDismiss();
    }

    private void handleDetails() {
        Intent intent = new Intent(this, com.example.do4e.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finishAndDismiss();
    }

    private void handleSkip() {
        finishAndDismiss();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void finishAndDismiss() {
        Intent serviceIntent = new Intent(this, AlarmSoundService.class);
        serviceIntent.setAction(AlarmSoundService.ACTION_STOP);
        startService(serviceIntent);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null)
            nm.cancel(notifId);
        finish();
    }
}