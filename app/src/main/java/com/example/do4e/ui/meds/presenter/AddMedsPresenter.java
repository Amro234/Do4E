package com.example.do4e.ui.meds.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.do4e.data.repository.MedRepository;
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.ReminderScheduler;

import java.util.Locale;

public class AddMedsPresenter implements AddMedsContract.Presenter {

    private final AddMedsContract.View view;
    private final MedRepository repository;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AddMedsPresenter(AddMedsContract.View view, Context context) {
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
                    view.populateForm(med);
                }
            });
        });
    }

    @Override
    public void saveMedicine(String name, String dosage, String notes, int hour, int minute, String interval, String frequency, int daysCount, boolean isContinuous, String medType, String instruction, long startDate, int existingMedId) {
        if (name == null || name.trim().isEmpty()) {
            view.showNameError("Medicine name is required");
            return;
        }
        view.showNameError(null);

        String amPm = hour >= 12 ? "PM" : "AM";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);

        int finalDays = isContinuous ? 0 : daysCount;

        MedEntity med = new MedEntity(name.trim(), dosage.trim(), timeStr, hour, minute, interval, frequency, finalDays, isContinuous, medType, instruction, notes.trim());
        med.startDate = startDate;

        if (existingMedId != -1) {
            med.id_meds = existingMedId;
        }

        if (existingMedId == -1) {
            repository.insertMedicine(med, newId -> {
                med.id_meds = newId.intValue();
                finishSave(med, name, timeStr, interval);
            });
        } else {
            repository.updateMedicine(med, () -> {
                finishSave(med, name, timeStr, interval);
            });
        }
    }

    private void finishSave(MedEntity med, String name, String timeStr, String interval) {
        try {
            ReminderScheduler.schedule(context, med);
            mainHandler.post(() -> view.showSuccessSheet(name, timeStr, interval));
        } catch (Exception e) {
            e.printStackTrace();
            mainHandler.post(() -> view.showErrorToast("Error saving: " + e.getMessage()));
        }
    }
}
