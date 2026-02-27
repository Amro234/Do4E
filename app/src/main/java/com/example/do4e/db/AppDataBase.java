package com.example.do4e.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = { MedEntity.class }, version = 3, exportSchema = false)
public abstract class AppDataBase extends RoomDatabase {

    public abstract MedDAO medDAO();

    private static AppDataBase instance;

    public static synchronized AppDataBase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDataBase.class,
                    "MedDatabase")
                    .fallbackToDestructiveMigration() // safe during dev — drops & recreates on schema change
                    .build();
        }
        return instance;
    }
}