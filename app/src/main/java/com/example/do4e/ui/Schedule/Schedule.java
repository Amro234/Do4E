package com.example.do4e.ui.Schedule;

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

        // Set the header date (e.g., "Monday, Oct 14")
        String dateStr = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(new Date());
        if (tvTodayDate != null)
            tvTodayDate.setText(dateStr);

        view.findViewById(R.id.btn_back)
                .setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        loadDailySchedule();
    }

    private void loadDailySchedule() {
        AppDataBase db = AppDataBase.getInstance(requireContext());
        long today = System.currentTimeMillis();
        new Thread(() -> {
            // Fetch only meds for today that haven't been completed
            List<MedEntity> todaysMeds = db.medDAO().getActiveMedsForToday(today);

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
        // Inflate a custom row for the timeline
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_schedule_timeline, medContainer, false);

        TextView tvTime = row.findViewById(R.id.tv_time_dot);
        TextView tvName = row.findViewById(R.id.tv_med_name);
        TextView tvDetail = row.findViewById(R.id.tv_med_detail);
        ImageView ivIcon = row.findViewById(R.id.iv_med_type_icon);

        tvTime.setText(med.time);
        tvName.setText(med.name);

        String detail = med.dosage + " • " + med.instruction;
        tvDetail.setText(detail);

        // Set icon based on type (using filenames found in drawable folder)
        if ("Syrup".equalsIgnoreCase(med.medType)) {
            ivIcon.setImageResource(R.drawable.serup_24dp_icon);
        } else if ("Syringe".equalsIgnoreCase(med.medType)) {
            ivIcon.setImageResource(R.drawable.syringe_24dp_icon);
        } else {
            ivIcon.setImageResource(R.drawable.pill_24dp_icon);
        }

        medContainer.addView(row);
    }
}