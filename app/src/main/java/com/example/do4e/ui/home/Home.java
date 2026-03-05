package com.example.do4e.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do4e.R;
import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedDAO;
import com.example.do4e.db.MedEntity;
import com.example.do4e.ui.home.HomeScheduleAdapter;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Home extends Fragment {

    private MedDAO medDAO;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private View homeLayout, emptyLayout;
    private RecyclerView rvTodaySchedule;
    private HomeScheduleAdapter scheduleAdapter;

    private MedEntity currentNextDose;
    private android.content.SharedPreferences trackingPrefs;

    private TextView tvNextMedName, tvNextMedTime, tvNextMedDosage;
    private TextView tvProgressPercent, tvMedStatusMsg, tvProgressFraction;
    private ProgressBar pbCircleProgress;
    private ViewGroup llProgressSegments;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Find views in the main layout (which is now a ScrollView containing both)
        // Wait, fragment_home.xml is just the UI. medicine_no.xml needs to be included
        // or swapped.
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize DB
        medDAO = AppDataBase.getInstance(requireContext()).medDAO();

        // Initialize Views
        rvTodaySchedule = view.findViewById(R.id.rvTodaySchedule);
        tvNextMedName = view.findViewById(R.id.tvMedName);
        tvNextMedTime = view.findViewById(R.id.tvNextDoseTime);
        tvNextMedDosage = view.findViewById(R.id.tvMedDosage);

        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        tvMedStatusMsg = view.findViewById(R.id.tvMedStatusMsg);
        tvProgressFraction = view.findViewById(R.id.tvProgressFraction);
        pbCircleProgress = view.findViewById(R.id.pbCircleProgress);
        llProgressSegments = view.findViewById(R.id.llProgressSegments);

        homeLayout = view.findViewById(R.id.home_content_container);

        trackingPrefs = requireContext().getSharedPreferences("DailyTracking",
                android.content.Context.MODE_PRIVATE);

        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void setupRecyclerView() {
        rvTodaySchedule
                .setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        scheduleAdapter = new HomeScheduleAdapter(med -> {
            scheduleAdapter.setSelectedMedId(med.id_meds);
            String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            boolean isTaken = trackingPrefs.getBoolean(todayKey + "_" + med.id_meds, false);
            updateHeroCard(med, isTaken, todayKey);
        });
        rvTodaySchedule.setAdapter(scheduleAdapter);
    }

    private void loadData() {
        executor.execute(() -> {
            // Get all active meds for today
            long todayTimestamp = getEndOfDay(new Date().getTime());
            List<MedEntity> todaysMeds = medDAO.getActiveMedsForToday(todayTimestamp);

            mainHandler.post(() -> {
                if (todaysMeds == null || todaysMeds.isEmpty()) {
                    showEmptyState();
                } else {
                    showHomeState();
                    updateUI(todaysMeds);
                }
            });
        });
    }

    private void showEmptyState() {
        if (homeLayout != null)
            homeLayout.setVisibility(View.GONE);
        ViewStub vs = getView().findViewById(R.id.vsEmptyState);
        if (vs != null) {
            emptyLayout = vs.inflate();
            // Hide the add button as requested
            View btnAdd = emptyLayout.findViewById(R.id.btn_add_medicine);
            if (btnAdd != null)
                btnAdd.setVisibility(View.GONE);
        } else if (emptyLayout != null) {
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showHomeState() {
        if (homeLayout != null)
            homeLayout.setVisibility(View.VISIBLE);
        if (emptyLayout != null)
            emptyLayout.setVisibility(View.GONE);
    }

    private void updateUI(List<MedEntity> meds) {

        // 1. Sort meds by time
        Collections.sort(meds, (m1, m2) -> {
            if (m1.hour != m2.hour)
                return m1.hour - m2.hour;
            return m1.minute - m2.minute;
        });

        String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        int total = meds.size();
        int taken = 0;
        currentNextDose = null;

        for (MedEntity m : meds) {
            String medKey = todayKey + "_" + m.id_meds;
            if (trackingPrefs.getBoolean(medKey, false)) {
                taken++;
            } else if (currentNextDose == null) {
                // First un-taken medicine becomes the Next Dose
                currentNextDose = m;
            }
        }

        // 2. Identify and display Next Dose (upcoming)
        if (currentNextDose != null) {
            scheduleAdapter.setSelectedMedId(currentNextDose.id_meds);
            updateHeroCard(currentNextDose, false, todayKey);
        } else if (!meds.isEmpty()) {
            scheduleAdapter.setSelectedMedId(-1);
            updateHeroCard(null, false, todayKey);
        }

        // 3. Progress Calculation
        int percent = total > 0 ? Math.round(((float) taken / total) * 100) : 0;
        tvProgressPercent.setText(percent + "%");
        pbCircleProgress.setProgress(percent);

        if (total == taken) {
            tvMedStatusMsg.setText("Great job! You have\ntaken all your doses\nfor today!");
        } else {
            tvMedStatusMsg
                    .setText("You have " + (total - taken) + " doses\nremaining for today. Keep\nup the consistency!");
        }

        // Ensure tvProgressFraction is updated
        if (tvProgressFraction != null)
            tvProgressFraction.setText(taken + "/" + total);

        // Update Progress Segments
        updateSegments(total, taken);

        // 4. Update RecyclerView
        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        scheduleAdapter.setCurrentTime(currentTime);
        scheduleAdapter.setMeds(meds);
    }

    private void updateHeroCard(MedEntity med, boolean isTaken, String todayKey) {
        View btnLogDose = requireView().findViewById(R.id.btnLogDose);
        if (med != null) {
            tvNextMedName.setText(med.name);
            tvNextMedTime.setText(med.time);
            tvNextMedDosage.setText(med.dosage + " • " + med.frequency + " Times");
            if (btnLogDose != null) {
                btnLogDose.setVisibility(View.VISIBLE);
                com.google.android.material.button.MaterialButton materialBtnLogDose = (com.google.android.material.button.MaterialButton) btnLogDose;
                if (isTaken) {
                    materialBtnLogDose.setText("Already Logged");
                    materialBtnLogDose.setEnabled(false);
                    materialBtnLogDose.setAlpha(0.6f);
                } else {
                    materialBtnLogDose.setText("Log Dose");
                    materialBtnLogDose.setEnabled(true);
                    materialBtnLogDose.setAlpha(1.0f);
                    materialBtnLogDose.setOnClickListener(v -> {
                        trackingPrefs.edit().putBoolean(todayKey + "_" + med.id_meds, true).apply();
                        executor.execute(() -> medDAO.incrementDaysTaken(med.id_meds));
                        android.widget.Toast.makeText(requireContext(), "Dose logged successfully!",
                                android.widget.Toast.LENGTH_SHORT).show();
                        loadData();
                    });
                }
            }
        } else {
            tvNextMedName.setText("All Done!");
            tvNextMedTime.setText("");
            tvNextMedDosage.setText("You have taken all your doses for today.");
            if (btnLogDose != null) {
                btnLogDose.setVisibility(View.GONE);
            }
        }
    }

    private void updateSegments(int total, int taken) {
        llProgressSegments.removeAllViews();
        for (int i = 0; i < total; i++) {
            View segment = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,
                    1.0f);
            params.setMargins(0, 0, i == total - 1 ? 0 : 8, 0);
            segment.setLayoutParams(params);

            if (i < taken) {
                segment.setBackgroundResource(R.drawable.bg_progress_segment_active);
            } else {
                segment.setBackgroundResource(R.drawable.bg_progress_segment_inactive);
            }
            llProgressSegments.addView(segment);
        }
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
