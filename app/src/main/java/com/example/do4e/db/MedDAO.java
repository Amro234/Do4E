package com.example.do4e.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MedDAO {

    // ── Write operations ──────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MedEntity med);

    @Update
    void update(MedEntity med);

    @Delete
    void delete(MedEntity med);

    // ── Read operations ───────────────────────────────────────

    /** All meds — used by my_medicines screen */
    @Transaction
    @Query("SELECT * FROM meds ORDER BY hour ASC, minute ASC")
    List<MedEntity> getAllMeds();

    /** Single med by ID — used for edit/delete flows */
    @Transaction
    @Query("SELECT * FROM meds WHERE id_meds = :id LIMIT 1")
    MedEntity getById(int id);

    /** Check if any meds exist — used to show/hide meds_no empty state */
    @Transaction
    @Query("SELECT COUNT(*) FROM meds")
    int getMedCount();

    /** Continuous meds only — for reminder rescheduling */
    @Transaction
    @Query("SELECT * FROM meds WHERE isContinuous = 1 ORDER BY hour ASC, minute ASC")
    List<MedEntity> getContinuousMeds();

    /** Active (non-expired) meds — durationDays not yet reached */
    @Transaction
    @Query("SELECT * FROM meds WHERE isContinuous = 1 OR daysTaken < durationDays ORDER BY hour ASC, minute ASC")
    List<MedEntity> getActiveMeds();

    /** Increment daysTaken by 1 when user logs a dose */
    @Query("UPDATE meds SET daysTaken = daysTaken + 1 WHERE id_meds = :id")
    void incrementDaysTaken(int id);

    /** Delete all meds — used by Reset in settings (if added later) */
    @Query("DELETE FROM meds")
    void deleteAll();
}