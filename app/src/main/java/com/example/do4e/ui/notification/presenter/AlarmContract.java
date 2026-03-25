package com.example.do4e.ui.notification.presenter;

import com.example.do4e.db.MedEntity;

public interface AlarmContract {
    interface View {
        void displayMedicineDetails(MedEntity med);
        void finishAndDismissAlarm(int notifId);
        void navigateToHome();
    }

    interface Presenter {
        void loadMedicine(int medId);
        void onLogTaken(int medId, int notifId);
        void onSnooze(MedEntity med, int notifId);
        void onSkip(int notifId);
        void onDetailsClicked(int notifId);
    }
}
