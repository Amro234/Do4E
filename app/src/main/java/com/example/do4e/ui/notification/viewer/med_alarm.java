package com.example.do4e.ui.notification.viewer;

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
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.AlarmSoundService;
import com.example.do4e.ui.notification.presenter.AlarmContract;
import com.example.do4e.ui.notification.presenter.AlarmPresenter;

public class med_alarm extends AppCompatActivity implements AlarmContract.View {

    private int medId = -1;
    private int notifId = -1;
    private MedEntity currentMed;
    private AlarmContract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.activity_med_alarm);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { }
        });

        presenter = new AlarmPresenter(this, this);

        medId = getIntent().getIntExtra("med_id", -1);
        notifId = getIntent().getIntExtra("notif_id", 0);

        presenter.loadMedicine(medId);

        findViewById(R.id.btn_log_taken).setOnClickListener(ClickSoundHelper.get(this).wrap(v -> presenter.onLogTaken(medId, notifId)));
        findViewById(R.id.btn_snooze).setOnClickListener(ClickSoundHelper.get(this).wrap(v -> presenter.onSnooze(currentMed, notifId)));
        findViewById(R.id.btn_details).setOnClickListener(ClickSoundHelper.get(this).wrap(v -> presenter.onDetailsClicked(notifId)));
        findViewById(R.id.btn_skip).setOnClickListener(ClickSoundHelper.get(this).wrap(v -> presenter.onSkip(notifId)));
    }

    @Override
    public void displayMedicineDetails(MedEntity med) {
        this.currentMed = med;

        ((TextView) findViewById(R.id.tv_alarm_med_name)).setText(med.name);

        TextView tvArabic = findViewById(R.id.tv_alarm_med_name_arabic);
        if (tvArabic != null) tvArabic.setText(med.name);

        TextView tvDosage = findViewById(R.id.tv_alarm_dosage);
        if (tvDosage != null) tvDosage.setText(med.dosage != null && !med.dosage.isEmpty() ? med.dosage : "—");

        TextView tvTime = findViewById(R.id.tv_alarm_time);
        if (tvTime != null) tvTime.setText(med.time);

        TextView tvInstruction = findViewById(R.id.tv_alarm_instruction);
        if (tvInstruction != null && med.instruction != null) tvInstruction.setText(med.instruction);

        ImageView ivIcon = findViewById(R.id.iv_alarm_med_icon);
        if (ivIcon != null) ivIcon.setImageResource(R.drawable.live_reminder_icon);

        ImageView ivPhoto = findViewById(R.id.iv_alarm_med_photo);
        if (ivPhoto != null) {
            if ("Syrup".equalsIgnoreCase(med.medType)) ivPhoto.setImageResource(R.drawable.syrup1);
            else if ("Syringe".equalsIgnoreCase(med.medType)) ivPhoto.setImageResource(R.drawable.syringes2);
            else ivPhoto.setImageResource(R.drawable.pill1);
        }

        ImageView ivInstructionIcon = findViewById(R.id.iv_alarm_instruction_icon);
        if (ivInstructionIcon != null) {
            if ("Before Food".equalsIgnoreCase(med.instruction)) ivInstructionIcon.setImageResource(R.drawable.before_food_24dp_icon);
            else if ("During Food".equalsIgnoreCase(med.instruction)) ivInstructionIcon.setImageResource(R.drawable.during_food_24dp_icon);
            else if ("After Food".equalsIgnoreCase(med.instruction)) ivInstructionIcon.setImageResource(R.drawable.after_food_24dp_icon);
        }
    }

    @Override
    public void finishAndDismissAlarm(int notifId) {
        Intent serviceIntent = new Intent(this, AlarmSoundService.class);
        serviceIntent.setAction(AlarmSoundService.ACTION_STOP);
        startService(serviceIntent);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notifId);

        finish();
    }

    @Override
    public void navigateToHome() {
        Intent intent = new Intent(this, com.example.do4e.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}