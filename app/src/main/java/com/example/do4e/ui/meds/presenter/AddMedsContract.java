package com.example.do4e.ui.meds.presenter;

import com.example.do4e.db.MedEntity;

public interface AddMedsContract {
    interface View {
        void populateForm(MedEntity med);
        void showNameError(String error);
        void showSuccessSheet(String medName, String time, String interval);
        void showErrorToast(String message);
    }

    interface Presenter {
        void loadMedicine(int medId);
        void saveMedicine(String name, String dosage, String notes, int hour, int minute, String interval, String frequency, int daysCount, boolean isContinuous, String medType, String instruction, long startDate, int existingMedId);
    }
}
