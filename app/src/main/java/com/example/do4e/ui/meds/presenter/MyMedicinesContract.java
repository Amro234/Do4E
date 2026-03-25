package com.example.do4e.ui.meds.presenter;

import com.example.do4e.db.MedEntity;
import java.util.List;

public interface MyMedicinesContract {

    interface View {
        void refreshTabs(List<MedEntity> todaysMeds, List<MedEntity> dailyMeds, List<MedEntity> weeklyMeds, List<MedEntity> monthlyMeds);
        void updateFabVisibility(boolean hasMeds);
    }

    interface Presenter {
        void loadMedicines();
        void deleteMedicine(MedEntity med);
        void logNowClicked(MedEntity med);
        boolean hasMedsForTab(String tabName);
    }
}
