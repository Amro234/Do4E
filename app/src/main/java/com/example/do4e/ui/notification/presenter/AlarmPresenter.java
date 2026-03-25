package com.example.do4e.ui.notification.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.do4e.data.repository.MedRepository;
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.ReminderScheduler;
import com.example.do4e.ui.settings.settingsPref;

import java.util.Calendar;

public class AlarmPresenter implements AlarmContract.Presenter {

    private final AlarmContract.View view;
    private final MedRepository repository;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AlarmPresenter(AlarmContract.View view, Context context) {
        this.view = view;
        this.context = context;
        this.repository = MedRepository.getInstance(context);
    }

    @Override
    public void loadMedicine(int medId) {
        if (medId == -1) return;

        repository.getMedicineById(medId, med -> {
            mainHandler.post(() -> {
                if (med != null) {
                    view.displayMedicineDetails(med);
                }
            });
        });
    }

    @Override
    public void onLogTaken(int medId, int notifId) {
        if (medId == -1) {
            view.finishAndDismissAlarm(notifId);
            return;
        }

        repository.incrementDose(medId, () -> {
            mainHandler.post(() -> view.finishAndDismissAlarm(notifId));
        });
    }

    @Override
    public void onSnooze(MedEntity med, int notifId) {
        if (med == null) {
            view.finishAndDismissAlarm(notifId);
            return;
        }

        int snoozeMinutes = settingsPref.getSnoozeMinutes(context);

        Calendar snooze = Calendar.getInstance();
        snooze.add(Calendar.MINUTE, snoozeMinutes);

        MedEntity snoozeMed = new MedEntity();
        snoozeMed.id_meds = med.id_meds;
        snoozeMed.name = med.name;
        snoozeMed.time = med.time;
        snoozeMed.hour = snooze.get(Calendar.HOUR_OF_DAY);
        snoozeMed.minute = snooze.get(Calendar.MINUTE);
        snoozeMed.startDate = snooze.getTimeInMillis();

        ReminderScheduler.schedule(context, snoozeMed);
        view.finishAndDismissAlarm(notifId);
    }

    @Override
    public void onSkip(int notifId) {
        view.finishAndDismissAlarm(notifId);
    }

    @Override
    public void onDetailsClicked(int notifId) {
        view.navigateToHome();
        view.finishAndDismissAlarm(notifId);
    }
}
