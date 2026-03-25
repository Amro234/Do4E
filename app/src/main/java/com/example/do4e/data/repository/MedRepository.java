package com.example.do4e.data.repository;

import android.content.Context;

import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedDAO;
import com.example.do4e.db.MedEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MedRepository {

    private final MedDAO medDAO;
    private final ExecutorService executor;

    // Singleton instance
    private static MedRepository instance;

    private MedRepository(Context context) {
        AppDataBase db = AppDataBase.getInstance(context);
        this.medDAO = db.medDAO();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized MedRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MedRepository(context);
        }
        return instance;
    }

    public interface RepositoryCallback<T> {
        void onComplete(T result);
    }

    // --- Read Operations ---

    public void getAllMedicines(RepositoryCallback<List<MedEntity>> callback) {
        executor.execute(() -> {
            List<MedEntity> meds = medDAO.getAllMeds();
            callback.onComplete(meds);
        });
    }

    public void getMedicineById(int id, RepositoryCallback<MedEntity> callback) {
        executor.execute(() -> {
            MedEntity med = medDAO.getById(id);
            callback.onComplete(med);
        });
    }

    public void getActiveMedsForToday(long todayTimestamp, RepositoryCallback<List<MedEntity>> callback) {
        executor.execute(() -> {
            List<MedEntity> meds = medDAO.getActiveMedsForToday(todayTimestamp);
            callback.onComplete(meds);
        });
    }

    public void getActiveMeds(RepositoryCallback<List<MedEntity>> callback) {
        executor.execute(() -> {
            List<MedEntity> meds = medDAO.getActiveMeds();
            callback.onComplete(meds);
        });
    }

    public void getMedCount(RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            int count = medDAO.getMedCount();
            callback.onComplete(count);
        });
    }

    // --- Write Operations ---

    public void insertMedicine(MedEntity med, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            long newId = medDAO.insert(med);
            if (callback != null)
                callback.onComplete(newId);
        });
    }

    public void updateMedicine(MedEntity med, Runnable onComplete) {
        executor.execute(() -> {
            medDAO.update(med);
            if (onComplete != null)
                onComplete.run();
        });
    }

    public void deleteMedicine(MedEntity med, Runnable onComplete) {
        executor.execute(() -> {
            medDAO.delete(med);
            if (onComplete != null)
                onComplete.run();
        });
    }

    public void incrementDose(int id, Runnable onComplete) {
        executor.execute(() -> {
            medDAO.incrementDaysTaken(id);
            if (onComplete != null)
                onComplete.run();
        });
    }
}
