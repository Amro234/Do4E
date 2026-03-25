package com.example.do4e.ui.Schedule.presenter;

import android.content.Context;

import com.example.do4e.data.repository.MedRepository;
import com.example.do4e.db.MedEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class SchedulePresenter implements ScheduleContract.Presenter {

    private final ScheduleContract.View view;
    private final MedRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public SchedulePresenter(ScheduleContract.View view, Context context) {
        this.view = view;
        this.repository = MedRepository.getInstance(context);
    }

    @Override
    public void loadDailySchedule() {
        String dateStr = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date());
        view.showDate(dateStr);

        disposables.add(
            repository.getActiveMeds()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(allActive -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    long todayLocal = cal.getTimeInMillis();

                    List<MedEntity> todaysMeds = new ArrayList<>();
                    for (MedEntity m : allActive) {
                        Calendar startCal = Calendar.getInstance();
                        startCal.setTimeInMillis(m.startDate);
                        startCal.set(Calendar.HOUR_OF_DAY, 0);
                        startCal.set(Calendar.MINUTE, 0);
                        startCal.set(Calendar.SECOND, 0);
                        startCal.set(Calendar.MILLISECOND, 0);
                        long startLocal = startCal.getTimeInMillis();

                        if (todayLocal >= startLocal) {
                            todaysMeds.add(m);
                        }
                    }

                    if (todaysMeds.isEmpty()) {
                        view.showEmptyState();
                    } else {
                        view.hideEmptyState();
                        view.showDailySchedule(todaysMeds);
                    }
                }, throwable -> view.showEmptyState())
        );
    }

    @Override
    public void updateMedTime(MedEntity med, int hourOfDay, int minute) {
        med.hour = hourOfDay;
        med.minute = minute;

        int h12 = hourOfDay % 12;
        if (h12 == 0) h12 = 12;
        String amPm = hourOfDay >= 12 ? "PM" : "AM";
        med.time = String.format(Locale.getDefault(), "%02d:%02d %s", h12, minute, amPm);

        disposables.add(
            repository.updateMedicine(med)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::loadDailySchedule,
                    throwable -> { /* silent fail for time update */ })
        );
    }

    public void dispose() {
        disposables.clear();
    }
}
