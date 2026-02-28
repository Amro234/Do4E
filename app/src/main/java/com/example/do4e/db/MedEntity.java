package com.example.do4e.db;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "meds")
public class MedEntity {

    @PrimaryKey(autoGenerate = true)
    public int id_meds;

    // ── General Info ──────────────────────────────────────────
    public String name; // Medicine name, e.g. "Panadol Extra"
    public String dosage; // Dosage string, e.g. "500mg"

    // ── Schedule & Frequency ─────────────────────────────────
    public String time; // Formatted time, e.g. "08:30 AM"
    public int hour; // 0-23
    public int minute; // 0-59
    public String interval; // "Daily" | "Weekly" | "Monthly"
    public String frequency; // 1 | 2 | 3 (times per interval)

    // ── Duration ─────────────────────────────────────────────
    public int durationDays; // Number of days, e.g. 7. 0 = continuous
    public boolean isContinuous; // true = no end date

    // ── Medicine Type ────────────────────────────────────────
    public String medType; // "Pill" | "Syrup" | "Syringe"

    // ── Instructions ────────────────────────────────────────
    public String instruction; // "Before Food" | "During Food" | "After Food"

    // ── Notes ────────────────────────────────────────────────
    public String notes; // Optional free-text notes

    // ── Progress tracking (used by my_medicines screen) ──────
    public int daysTaken; // How many days the user has already taken it

    /** No-arg constructor for Room */
    public MedEntity() {
    }

    @Ignore
    public MedEntity(String name, String dosage, String time, int hour, int minute,
            String interval, String frequency, int durationDays, boolean isContinuous,
            String medType, String instruction, String notes) {
        this.name = name;
        this.dosage = dosage;
        this.time = time;
        this.hour = hour;
        this.minute = minute;
        this.interval = interval;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.isContinuous = isContinuous;
        this.medType = medType;
        this.instruction = instruction;
        this.notes = notes;
        this.daysTaken = 0;
    }
}