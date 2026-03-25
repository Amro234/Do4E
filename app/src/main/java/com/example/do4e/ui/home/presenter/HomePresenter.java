package com.example.do4e.ui.home.presenter;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.do4e.data.repository.MedRepository;
import com.example.do4e.db.MedEntity;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class HomePresenter implements HomeContract.Presenter {

    private final HomeContract.View view;
    private final MedRepository repository;
    private final SharedPreferences trackingPrefs;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public HomePresenter(HomeContract.View view, Context context) {
        this.view = view;
        this.repository = MedRepository.getInstance(context);
        this.trackingPrefs = context.getSharedPreferences("DailyTracking", Context.MODE_PRIVATE);
    }

    @Override
    public void loadData() {
        long todayTimestamp = getEndOfDay(new Date().getTime());

        disposables.add(
            repository.getActiveMedsForToday(todayTimestamp)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(meds -> {
                    if (meds == null || meds.isEmpty()) {
                        view.showEmptyState();
                    } else {
                        view.showHomeState();
                        processMedsAndDisplay(meds);
                    }
                }, throwable -> view.showMessage("Error loading data"))
        );
    }

    @Override
    public void onDoseLogged(MedEntity med, String todayKey) {
        trackingPrefs.edit().putBoolean(todayKey + "_" + med.id_meds, true).apply();

        disposables.add(
            repository.incrementDose(med.id_meds)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    view.showMessage("Dose logged successfully!");
                    loadData();
                }, throwable -> view.showMessage("Error logging dose"))
        );
    }

    @Override
    public void onMedSelected(MedEntity med) {
        String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        boolean isTaken = trackingPrefs.getBoolean(todayKey + "_" + med.id_meds, false);
        view.selectScheduleMed(med.id_meds);
        view.updateHeroCard(med, isTaken, todayKey);
    }

    public void dispose() {
        disposables.clear();
    }

    private void processMedsAndDisplay(List<MedEntity> meds) {
        Collections.sort(meds, (m1, m2) -> {
            if (m1.hour != m2.hour)
                return m1.hour - m2.hour;
            return m1.minute - m2.minute;
        });

        String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        int total = meds.size();
        int taken = 0;
        MedEntity currentNextDose = null;

        for (MedEntity m : meds) {
            String medKey = todayKey + "_" + m.id_meds;
            if (trackingPrefs.getBoolean(medKey, false)) {
                taken++;
            } else if (currentNextDose == null) {
                currentNextDose = m;
            }
        }

        if (currentNextDose != null) {
            view.selectScheduleMed(currentNextDose.id_meds);
            view.updateHeroCard(currentNextDose, false, todayKey);
        } else if (!meds.isEmpty()) {
            view.selectScheduleMed(-1);
            view.updateHeroCard(null, false, todayKey);
        }

        int percent = total > 0 ? Math.round(((float) taken / total) * 100) : 0;
        view.updateProgress(percent, total, taken);

        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        view.updateScheduleList(meds, currentTime);
    }

    private long getEndOfDay(long timestamp) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        cal.set(java.util.Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}
