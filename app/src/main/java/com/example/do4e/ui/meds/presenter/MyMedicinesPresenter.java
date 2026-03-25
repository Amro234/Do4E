package com.example.do4e.ui.meds.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.do4e.data.repository.MedRepository;
import com.example.do4e.db.MedEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MyMedicinesPresenter implements MyMedicinesContract.Presenter {

    private final MyMedicinesContract.View view;
    private final MedRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<MedEntity> allMeds = new ArrayList<>();

    public MyMedicinesPresenter(MyMedicinesContract.View view, Context context) {
        this.view = view;
        this.repository = MedRepository.getInstance(context);
    }

    @Override
    public void loadMedicines() {
        repository.getAllMedicines(meds -> {
            allMeds = meds;

            List<MedEntity> todaysMeds = filterMeds("Today");
            List<MedEntity> dailyMeds = filterMeds("Daily");
            List<MedEntity> weeklyMeds = filterMeds("Weekly");
            List<MedEntity> monthlyMeds = filterMeds("Monthly");

            mainHandler.post(() -> {
                view.refreshTabs(todaysMeds, dailyMeds, weeklyMeds, monthlyMeds);
            });
        });
    }

    @Override
    public void deleteMedicine(MedEntity med) {
        repository.deleteMedicine(med, () -> {
            mainHandler.post(this::loadMedicines);
        });
    }

    @Override
    public void logNowClicked(MedEntity med) {
        repository.incrementDose(med.id_meds, () -> {
            mainHandler.post(this::loadMedicines);
        });
    }

    @Override
    public boolean hasMedsForTab(String tabName) {
        return !filterMeds(tabName).isEmpty();
    }

    private List<MedEntity> filterMeds(String tab) {
        List<MedEntity> filtered = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayLocal = cal.getTimeInMillis();

        for (MedEntity m : allMeds) {
            String interval = m.interval == null || m.interval.isEmpty() ? "Daily" : m.interval;

            Calendar startCal = Calendar.getInstance();
            startCal.setTimeInMillis(m.startDate);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            long startLocal = startCal.getTimeInMillis();

            if (tab.equals("Today")) {
                if (todayLocal >= startLocal && (m.isContinuous || m.daysTaken < m.durationDays))
                    filtered.add(m);
            } else {
                if (interval.equals(tab))
                    filtered.add(m);
            }
        }
        return filtered;
    }
}
