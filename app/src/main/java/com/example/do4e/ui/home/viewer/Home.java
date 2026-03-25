package com.example.do4e.ui.home.viewer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do4e.R;
import com.example.do4e.db.MedEntity;
import com.example.do4e.ui.home.presenter.HomeContract;
import com.example.do4e.ui.home.presenter.HomePresenter;

import java.util.List;

public class Home extends Fragment implements HomeContract.View {

    private HomeContract.Presenter presenter;

    private View homeLayout, emptyLayout;
    private RecyclerView rvTodaySchedule;
    private HomeScheduleAdapter scheduleAdapter;

    private TextView tvNextMedName, tvNextMedTime, tvNextMedDosage;
    private TextView tvProgressPercent, tvMedStatusMsg, tvProgressFraction;
    private ProgressBar pbCircleProgress;
    private ViewGroup llProgressSegments;
    private View btnLogDose;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTodaySchedule = view.findViewById(R.id.rvTodaySchedule);
        tvNextMedName = view.findViewById(R.id.tvMedName);
        tvNextMedTime = view.findViewById(R.id.tvNextDoseTime);
        tvNextMedDosage = view.findViewById(R.id.tvMedDosage);
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        tvMedStatusMsg = view.findViewById(R.id.tvMedStatusMsg);
        tvProgressFraction = view.findViewById(R.id.tvProgressFraction);
        pbCircleProgress = view.findViewById(R.id.pbCircleProgress);
        llProgressSegments = view.findViewById(R.id.llProgressSegments);
        btnLogDose = view.findViewById(R.id.btnLogDose);
        homeLayout = view.findViewById(R.id.home_content_container);

        presenter = new HomePresenter(this, requireContext());
        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.loadData();
    }

    private void setupRecyclerView() {
        rvTodaySchedule.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        scheduleAdapter = new HomeScheduleAdapter(med -> presenter.onMedSelected(med));
        rvTodaySchedule.setAdapter(scheduleAdapter);
    }

    @Override
    public void showEmptyState() {
        if (homeLayout != null) homeLayout.setVisibility(View.GONE);
        ViewStub vs = getView().findViewById(R.id.vsEmptyState);
        if (vs != null) {
            emptyLayout = vs.inflate();
            View btnAdd = emptyLayout.findViewById(R.id.btn_add_medicine);
            if (btnAdd != null) btnAdd.setVisibility(View.GONE);
        } else if (emptyLayout != null) {
            emptyLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showHomeState() {
        if (homeLayout != null) homeLayout.setVisibility(View.VISIBLE);
        if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
    }

    @Override
    public void updateScheduleList(List<MedEntity> meds, String currentTime) {
        scheduleAdapter.setCurrentTime(currentTime);
        scheduleAdapter.setMeds(meds);
    }

    @Override
    public void selectScheduleMed(int medId) {
        scheduleAdapter.setSelectedMedId(medId);
    }

    @Override
    public void updateHeroCard(MedEntity med, boolean isTaken, String todayKey) {
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
                    materialBtnLogDose.setOnClickListener(v -> presenter.onDoseLogged(med, todayKey));
                }
            }
        } else {
            tvNextMedName.setText("All Done!");
            tvNextMedTime.setText("");
            tvNextMedDosage.setText("You have taken all your doses for today.");
            if (btnLogDose != null) btnLogDose.setVisibility(View.GONE);
        }
    }

    @Override
    public void updateProgress(int percent, int total, int taken) {
        tvProgressPercent.setText(percent + "%");
        pbCircleProgress.setProgress(percent);
        if (total == taken) {
            tvMedStatusMsg.setText("Great job! You have\ntaken all your doses\nfor today!");
        } else {
            tvMedStatusMsg.setText("You have " + (total - taken) + " doses\nremaining for today. Keep\nup the consistency!");
        }
        if (tvProgressFraction != null) tvProgressFraction.setText(taken + "/" + total);
        updateSegments(total, taken);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateSegments(int total, int taken) {
        llProgressSegments.removeAllViews();
        for (int i = 0; i < total; i++) {
            View segment = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            params.setMargins(0, 0, i == total - 1 ? 0 : 8, 0);
            segment.setLayoutParams(params);
            segment.setBackgroundResource(i < taken ? R.drawable.bg_progress_segment_active : R.drawable.bg_progress_segment_inactive);
            llProgressSegments.addView(segment);
        }
    }
}
