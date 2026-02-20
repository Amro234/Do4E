package com.example.do4e.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "meds")
public class MedEntity {

    @PrimaryKey(autoGenerate = true)
    public int id_meds;

    public String name;
    public String time;

    public MedEntity(String name, String time) {
        this.name = name;
        this.time = time;
    }
}
