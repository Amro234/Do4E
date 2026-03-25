package com.example.do4e.ui.home.presenter;

import com.example.do4e.db.MedEntity;
import java.util.List;

public interface HomeContract {

    interface View {
        void showEmptyState();
        void showHomeState();
        void updateScheduleList(List<MedEntity> meds, String currentTime);
        void selectScheduleMed(int medId);
        void updateHeroCard(MedEntity med, boolean isTaken, String todayKey);
        void updateProgress(int percent, int total, int taken);
        void showMessage(String message);
    }

    interface Presenter {
        void loadData();
        void onDoseLogged(MedEntity med, String todayKey);
        void onMedSelected(MedEntity med);
    }
}
