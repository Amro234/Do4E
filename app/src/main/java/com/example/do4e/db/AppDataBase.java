package com.example.do4e.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = { MedEntity.class }, version = 4, exportSchema = false)
public abstract class AppDataBase extends RoomDatabase {

    public abstract MedDAO medDAO();

    private static AppDataBase instance;

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Strings can be null, so we just add them as TEXT
            database.execSQL("ALTER TABLE meds ADD COLUMN dosage TEXT");
            database.execSQL("ALTER TABLE meds ADD COLUMN interval TEXT");
            database.execSQL("ALTER TABLE meds ADD COLUMN frequency TEXT");
            database.execSQL("ALTER TABLE meds ADD COLUMN medType TEXT");
            database.execSQL("ALTER TABLE meds ADD COLUMN instruction TEXT");
            database.execSQL("ALTER TABLE meds ADD COLUMN notes TEXT");

            // Primitives (int, boolean) cannot be null in Room, so we must define them as
            // NOT NULL with a default value
            database.execSQL("ALTER TABLE meds ADD COLUMN hour INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE meds ADD COLUMN minute INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE meds ADD COLUMN durationDays INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE meds ADD COLUMN isContinuous INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE meds ADD COLUMN daysTaken INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE meds ADD COLUMN startDate INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static synchronized AppDataBase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDataBase.class,
                    "MedDatabase")
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build();
        }
        return instance;
    }
}