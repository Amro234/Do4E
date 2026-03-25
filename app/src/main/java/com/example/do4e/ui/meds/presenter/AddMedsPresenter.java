package com.example.do4e.ui.meds.presenter;

import android.content.Context;

import com.example.do4e.data.repository.MedRepository;
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.ReminderScheduler;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class AddMedsPresenter implements AddMedsContract.Presenter {

    private final AddMedsContract.View view;
    private final MedRepository repository;
    private final Context context;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public AddMedsPresenter(AddMedsContract.View view, Context context) {
        this.view = view;
        this.context = context;
        this.repository = MedRepository.getInstance(context);
    }

    @Override
    public void loadMedicine(int medId) {
        if (medId == -1) return;

        disposables.add(
            repository.getMedicineById(medId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(med -> {
                    if (med != null) {
                        view.populateForm(med);
                    }
                }, throwable -> view.showErrorToast("Error loading medicine"))
        );
    }

    @Override
    public void saveMedicine(String name, String dosage, String notes, int hour, int minute,
                             String interval, String frequency, int daysCount, boolean isContinuous,
                             String medType, String instruction, long startDate, int existingMedId) {
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

        MedEntity med = new MedEntity(name.trim(), dosage.trim(), timeStr, hour, minute,
                interval, frequency, finalDays, isContinuous, medType, instruction, notes.trim());
        med.startDate = startDate;

        if (existingMedId != -1) {
            med.id_meds = existingMedId;
        }

        if (existingMedId == -1) {
            disposables.add(
                repository.insertMedicine(med)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(newId -> {
                        med.id_meds = newId.intValue();
                        finishSave(med, name, timeStr, interval);
                    }, throwable -> view.showErrorToast("Error saving: " + throwable.getMessage()))
            );
        } else {
            disposables.add(
                repository.updateMedicine(med)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        () -> finishSave(med, name, timeStr, interval),
                        throwable -> view.showErrorToast("Error updating: " + throwable.getMessage()))
            );
        }
    }

    private void finishSave(MedEntity med, String name, String timeStr, String interval) {
        try {
            ReminderScheduler.schedule(context, med);
            view.showSuccessSheet(name, timeStr, interval);
        } catch (Exception e) {
            e.printStackTrace();
            view.showErrorToast("Error saving: " + e.getMessage());
        }
    }

    public void dispose() {
        disposables.clear();
    }
}
