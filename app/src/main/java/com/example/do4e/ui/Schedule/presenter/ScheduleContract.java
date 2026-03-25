package com.example.do4e.ui.Schedule.presenter;

import com.example.do4e.db.MedEntity;
import java.util.List;

public interface ScheduleContract {

    interface View {
        void showEmptyState();
        void hideEmptyState();
        void showDate(String dateStr);
        void showDailySchedule(List<MedEntity> meds);
    }

    interface Presenter {
        void loadDailySchedule();
        void updateMedTime(MedEntity med, int hourOfDay, int minute);
    }
}
