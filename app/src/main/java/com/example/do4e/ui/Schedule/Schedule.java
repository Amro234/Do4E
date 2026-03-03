package com.example.do4e.ui.Schedule;

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
import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Schedule extends Fragment {

    private LinearLayout medContainer;
    private View emptyHint;
    private TextView tvTodayDate;

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

        String dateStr = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date());
        if (tvTodayDate != null)
            tvTodayDate.setText(dateStr);

        view.findViewById(R.id.btn_back)
                .setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        loadDailySchedule();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDailySchedule();
    }

    private void loadDailySchedule() {
        AppDataBase db = AppDataBase.getInstance(requireContext());

        new Thread(() -> {
            // Fetch ALL active meds (no startDate filter in SQL)
            List<MedEntity> allActive = db.medDAO().getActiveMeds();

            // Normalize "today" to midnight local time
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long todayLocal = cal.getTimeInMillis();

            // Filter in Java: normalize each med's startDate to local midnight
            List<MedEntity> todaysMeds = new ArrayList<>();
            for (MedEntity m : allActive) {
                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(m.startDate);
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                startCal.set(Calendar.MILLISECOND, 0);
                long startLocal = startCal.getTimeInMillis();

                if (todayLocal >= startLocal) {
                    todaysMeds.add(m);
                }
            }

            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    medContainer.removeAllViews();
                    if (todaysMeds.isEmpty()) {
                        emptyHint.setVisibility(View.VISIBLE);
                    } else {
                        emptyHint.setVisibility(View.GONE);
                        for (MedEntity med : todaysMeds) {
                            inflateMedRow(med);
                        }
                    }
                });
            }
        }).start();
    }

    private void inflateMedRow(MedEntity med) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_schedule_timeline, medContainer, false);

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

        // ── Edit time button ──────────────────────────────────────────────
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                new TimePickerDialog(requireContext(), (picker, hourOfDay, minute) -> {
                    med.hour = hourOfDay;
                    med.minute = minute;

                    int h12 = hourOfDay % 12;
                    if (h12 == 0)
                        h12 = 12;
                    String amPm = hourOfDay >= 12 ? "PM" : "AM";
                    med.time = String.format(Locale.getDefault(), "%02d:%02d %s", h12, minute, amPm);

                    tvTime.setText(med.time);

                    // Persist to DB
                    AppDataBase db = AppDataBase.getInstance(requireContext());
                    new Thread(() -> {
                        db.medDAO().update(med);
                    }).start();

                }, med.hour, med.minute, false).show();
            });
        }

        medContainer.addView(row);
    }
}