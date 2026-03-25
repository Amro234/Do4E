package com.example.do4e.ui.Schedule.viewer;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.do4e.R;
import com.example.do4e.db.MedEntity;
import com.example.do4e.ui.Schedule.presenter.ScheduleContract;
import com.example.do4e.ui.Schedule.presenter.SchedulePresenter;

import java.util.List;

public class Schedule extends Fragment implements ScheduleContract.View {

    private LinearLayout medContainer;
    private View emptyHint;
    private TextView tvTodayDate;
    private ScheduleContract.Presenter presenter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        medContainer = view.findViewById(R.id.med_container);
        emptyHint = view.findViewById(R.id.empty_hint_container);
        tvTodayDate = view.findViewById(R.id.tv_today_date);

        presenter = new SchedulePresenter(this, requireContext());

        view.findViewById(R.id.btn_back).setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        presenter.loadDailySchedule();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.loadDailySchedule();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (presenter instanceof SchedulePresenter) {
            ((SchedulePresenter) presenter).dispose();
        }
    }

    @Override
    public void showEmptyState() {
        medContainer.removeAllViews();
        emptyHint.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideEmptyState() {
        emptyHint.setVisibility(View.GONE);
    }

    @Override
    public void showDate(String dateStr) {
        if (tvTodayDate != null) tvTodayDate.setText(dateStr);
    }

    @Override
    public void showDailySchedule(List<MedEntity> meds) {
        medContainer.removeAllViews();
        for (MedEntity med : meds) {
            inflateMedRow(med);
        }
    }

    private void inflateMedRow(MedEntity med) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_schedule_timeline, medContainer, false);

        TextView tvTime = row.findViewById(R.id.tv_time_dot);
        TextView tvName = row.findViewById(R.id.tv_med_name);
        TextView tvDetail = row.findViewById(R.id.tv_med_detail);
        ImageView ivIcon = row.findViewById(R.id.iv_med_type_icon);
        View btnEdit = row.findViewById(R.id.btn_edit_time);

        tvTime.setText(med.time);
        tvName.setText(med.name);
        tvDetail.setText(med.dosage + " • " + med.instruction);

        if ("Syrup".equalsIgnoreCase(med.medType))
            ivIcon.setImageResource(R.drawable.serup_24dp_icon);
        else if ("Syringe".equalsIgnoreCase(med.medType))
            ivIcon.setImageResource(R.drawable.syringe_24dp_icon);
        else
            ivIcon.setImageResource(R.drawable.pill_24dp_icon);

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                new TimePickerDialog(requireContext(), (picker, hourOfDay, minute) -> {
                    presenter.updateMedTime(med, hourOfDay, minute);
                }, med.hour, med.minute, false).show();
            });
        }

        medContainer.addView(row);
    }
}