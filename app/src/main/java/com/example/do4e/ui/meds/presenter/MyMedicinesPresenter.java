package com.example.do4e.ui.meds.presenter;

import android.content.Context;

import com.example.do4e.data.repository.MedRepository;
import com.example.do4e.db.MedEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class MyMedicinesPresenter implements MyMedicinesContract.Presenter {

    private final MyMedicinesContract.View view;
    private final MedRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private List<MedEntity> allMeds = new ArrayList<>();

    public MyMedicinesPresenter(MyMedicinesContract.View view, Context context) {
        this.view = view;
        this.repository = MedRepository.getInstance(context);
    }

    @Override
    public void loadMedicines() {
        disposables.add(
            repository.getAllMedicines()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(meds -> {
                    allMeds = meds;

                    List<MedEntity> todaysMeds = filterMeds("Today");
                    List<MedEntity> dailyMeds = filterMeds("Daily");
                    List<MedEntity> weeklyMeds = filterMeds("Weekly");
                    List<MedEntity> monthlyMeds = filterMeds("Monthly");

                    view.refreshTabs(todaysMeds, dailyMeds, weeklyMeds, monthlyMeds);
                }, throwable -> { /* handle error if needed */ })
        );
    }

    @Override
    public void deleteMedicine(MedEntity med) {
        disposables.add(
            repository.deleteMedicine(med)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadMedicines,
                    throwable -> { /* handle error */ })
        );
    }

    @Override
    public void logNowClicked(MedEntity med) {
        disposables.add(
            repository.incrementDose(med.id_meds)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadMedicines,
                    throwable -> { /* handle error */ })
        );
    }

    @Override
    public boolean hasMedsForTab(String tabName) {
        return !filterMeds(tabName).isEmpty();
    }

    public void dispose() {
        disposables.clear();
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
