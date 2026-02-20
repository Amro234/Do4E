package com.example.do4e.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MedDAO {

    @Insert
    void insert(MedEntity med);

    @androidx.room.Update
    void update(MedEntity med);

    @androidx.room.Delete
    void delete(MedEntity med);

    @Query("SELECT * FROM meds")
    List<MedEntity> getAllMeds();
}
