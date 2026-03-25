package com.example.do4e.data.repository;

import android.content.Context;

import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedDAO;
import com.example.do4e.db.MedEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Central data source for medicine operations.
 * All methods return cold RxJava streams that execute on Schedulers.io().
 * Callers are responsible for observing on the main thread.
 */
public class MedRepository {

    private final MedDAO medDAO;
    private static MedRepository instance;

    private MedRepository(Context context) {
        AppDataBase db = AppDataBase.getInstance(context);
        this.medDAO = db.medDAO();
    }

    public static synchronized MedRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MedRepository(context);
        }
        return instance;
    }

    // ── Read Operations ──────────────────────────────────────────────

    public Single<List<MedEntity>> getAllMedicines() {
        return Single.fromCallable(medDAO::getAllMeds)
                .subscribeOn(Schedulers.io());
    }

    public Single<MedEntity> getMedicineById(int id) {
        return Single.fromCallable(() -> medDAO.getById(id))
                .subscribeOn(Schedulers.io());
    }

    public Single<List<MedEntity>> getActiveMedsForToday(long todayTimestamp) {
        return Single.fromCallable(() -> medDAO.getActiveMedsForToday(todayTimestamp))
                .subscribeOn(Schedulers.io());
    }

    public Single<List<MedEntity>> getActiveMeds() {
        return Single.fromCallable(medDAO::getActiveMeds)
                .subscribeOn(Schedulers.io());
    }

    public Single<Integer> getMedCount() {
        return Single.fromCallable(medDAO::getMedCount)
                .subscribeOn(Schedulers.io());
    }

    // ── Write Operations ─────────────────────────────────────────────

    public Single<Long> insertMedicine(MedEntity med) {
        return Single.fromCallable(() -> medDAO.insert(med))
                .subscribeOn(Schedulers.io());
    }

    public Completable updateMedicine(MedEntity med) {
        return Completable.fromAction(() -> medDAO.update(med))
                .subscribeOn(Schedulers.io());
    }

    public Completable deleteMedicine(MedEntity med) {
        return Completable.fromAction(() -> medDAO.delete(med))
                .subscribeOn(Schedulers.io());
    }

    public Completable incrementDose(int id) {
        return Completable.fromAction(() -> medDAO.incrementDaysTaken(id))
                .subscribeOn(Schedulers.io());
    }
}
