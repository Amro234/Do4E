package com.example.do4e.ui.meds;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do4e.R;
import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.ReminderScheduler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class my_medicines extends Fragment {

    private MedAdapter adapter;

    // Views toggled between empty state and list state
    private View medicineNoLayout;
    private View medicinesListLayout;
    private View fabAdd;
    private View btnAddMedicine;
    private TextView tvNoMedicineTitle;

    private TextView tabToday, tabDaily, tabWeekly, tabMonthly;
    private String currentTab = "Today"; // "Today", "Daily", "Weekly", "Monthly"
    private List<MedEntity> allMeds = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_medicines, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if we should start on a specific tab (e.g. Daily, Weekly, Monthly)
        if (getArguments() != null) {
            String startTab = getArguments().getString("start_interval");
            if (startTab != null) {
                // interval values (Daily/Weekly/Monthly) map to their own tabs
                currentTab = startTab;
            }
        }

        // Cache views
        medicineNoLayout = view.findViewById(R.id.medicine_no_layout);
        medicinesListLayout = view.findViewById(R.id.medicines_list_layout);
        fabAdd = view.findViewById(R.id.fab_add);
        btnAddMedicine = view.findViewById(R.id.btn_add_medicine);
        tvNoMedicineTitle = view.findViewById(R.id.tv_no_medicine_title);

        // Back button
        view.findViewById(R.id.btn_back)
                .setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        // FAB (list state) → navigate to add_meds
        fabAdd.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.action_my_medicines_to_add_meds));

        // Empty-state "Add Medicine" button → navigate to add_meds
        btnAddMedicine.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.action_my_medicines_to_add_meds));

        // RecyclerView setup
        RecyclerView recyclerView = view.findViewById(R.id.rv_medicines);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new MedAdapter(new ArrayList<>(), this::onDeleteClicked, this::onLogNowClicked,
                    this::onEditClicked);
            recyclerView.setAdapter(adapter);
        }

        // Tabs setup
        tabToday = view.findViewById(R.id.tab_today);
        tabDaily = view.findViewById(R.id.tab_daily);
        tabWeekly = view.findViewById(R.id.tab_weekly);
        tabMonthly = view.findViewById(R.id.tab_monthly);

        tabToday.setOnClickListener(v -> selectTab("Today"));
        tabDaily.setOnClickListener(v -> selectTab("Daily"));
        tabWeekly.setOnClickListener(v -> selectTab("Weekly"));
        tabMonthly.setOnClickListener(v -> selectTab("Monthly"));

        loadMedicines();
    }

    private void selectTab(String tab) {
        currentTab = tab;
        updateTabStyles();
        filterAndDisplay();
    }

    private void updateTabStyles() {
        boolean isToday = currentTab.equals("Today");
        tabToday.setBackgroundResource(isToday ? R.drawable.bg_tab_selected : 0);
        tabToday.setTextColor(ContextCompat.getColor(requireContext(),
                isToday ? R.color.junglegreen : R.color.StateGray));

        boolean isDaily = currentTab.equals("Daily");
        tabDaily.setBackgroundResource(isDaily ? R.drawable.bg_tab_selected : 0);
        tabDaily.setTextColor(ContextCompat.getColor(requireContext(),
                isDaily ? R.color.junglegreen : R.color.StateGray));

        boolean isWeekly = currentTab.equals("Weekly");
        tabWeekly.setBackgroundResource(isWeekly ? R.drawable.bg_tab_selected : 0);
        tabWeekly.setTextColor(ContextCompat.getColor(requireContext(),
                isWeekly ? R.color.junglegreen : R.color.StateGray));

        boolean isMonthly = currentTab.equals("Monthly");
        tabMonthly.setBackgroundResource(isMonthly ? R.drawable.bg_tab_selected : 0);
        tabMonthly.setTextColor(ContextCompat.getColor(requireContext(),
                isMonthly ? R.color.junglegreen : R.color.StateGray));
    }

    private void filterAndDisplay() {
        List<MedEntity> filtered = new ArrayList<>();
        long today = System.currentTimeMillis();
        for (MedEntity m : allMeds) {
            if (m.interval == null || m.interval.isEmpty()) {
                m.interval = "Daily";
            }
            if (currentTab.equals("Today")) {
                // Show if it has started and hasn't expired
                if (today >= m.startDate && (m.isContinuous || m.daysTaken < m.durationDays)) {
                    filtered.add(m);
                }
            } else {
                if (m.interval.equals(currentTab)) {
                    filtered.add(m);
                }
            }
        }
        if (adapter != null) {
            adapter.updateList(filtered);
        }
        updateEmptyState(filtered.isEmpty());
    }

    private void onEditClicked(MedEntity med) {
        Bundle args = new Bundle();
        args.putInt("med_id", med.id_meds);
        NavHostFragment.findNavController(this).navigate(R.id.action_my_medicines_to_add_meds, args);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMedicines();
    }

    // ─── Load from DB ─────────────────────────────────────────────────────────

    private void loadMedicines() {
        AppDataBase db = AppDataBase.getInstance(requireContext());
        long today = System.currentTimeMillis();
        new Thread(() -> {
            // Fetch all meds so that future meds show in Daily/Weekly/Monthly
            List<MedEntity> meds = db.medDAO().getAllMeds();
            // 2. CHECK LIFECYCLE BEFORE TOUCHING UI
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    // 3. Safe to update UI
                    allMeds = meds;
                    filterAndDisplay();
                });
            }
        }).start();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            medicineNoLayout.setVisibility(View.VISIBLE);
            medicinesListLayout.setVisibility(View.GONE);
            fabAdd.setVisibility(View.GONE);

            if (tvNoMedicineTitle != null) {
                if (currentTab.equals("Daily")) {
                    tvNoMedicineTitle.setText(R.string.Medicine_No_Daily);
                } else if (currentTab.equals("Weekly")) {
                    tvNoMedicineTitle.setText(R.string.Medicine_NO_Weekly);
                } else if (currentTab.equals("Monthly")) {
                    tvNoMedicineTitle.setText(R.string.Medicine_No_Monthly);
                } else {
                    tvNoMedicineTitle.setText(R.string.Medicine_No);
                }
            }
        } else {
            medicineNoLayout.setVisibility(View.GONE);
            medicinesListLayout.setVisibility(View.VISIBLE);
            fabAdd.setVisibility(View.VISIBLE);
        }
    }

    // ─── Delete flow ──────────────────────────────────────────────────────────

    private void onDeleteClicked(MedEntity med) {
        // Pass the primitive ID instead of the whole object
        DeleteSheet sheet = DeleteSheet.newInstance(med.id_meds);
        sheet.setOnDeleteConfirmed(() -> loadMedicines());
        sheet.show(getParentFragmentManager(), "delete_sheet");
    }

    public static class DeleteSheet extends BottomSheetDialogFragment {

        private Runnable onConfirmed;
        private MedEntity med; // Fetched safely from DB

        // <-- UPDATED TO PASS ID VIA BUNDLE -->
        public static DeleteSheet newInstance(int medId) {
            DeleteSheet sheet = new DeleteSheet();
            Bundle args = new Bundle();
            args.putInt("MED_ID", medId);
            sheet.setArguments(args);
            return sheet;
        }

        public void setOnDeleteConfirmed(Runnable callback) {
            this.onConfirmed = callback;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.bottom_sheet_delete, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            if (getArguments() == null) {
                dismiss();
                return;
            }

            int medId = getArguments().getInt("MED_ID");
            AppDataBase db = AppDataBase.getInstance(requireContext());

            // <-- FETCH DATA FROM DB TO PREVENT PROCESS DEATH CRASH -->
            new Thread(() -> {
                try {
                    // Try direct fetch if you have getById in your DAO
                    med = db.medDAO().getById(medId);
                } catch (Exception e) {
                    // Fallback in case getById isn't set up: find it manually
                    for (MedEntity m : db.medDAO().getAllMeds()) {
                        if (m.id_meds == medId) {
                            med = m;
                            break;
                        }
                    }
                }

                // 2. CHECK LIFECYCLE BEFORE TOUCHING UI
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        // 3. Safe to update UI
                        if (med == null) {
                            dismiss();
                            return;
                        }
                        setupSheetUI(view);
                    });
                }
            }).start();
        }

        private void setupSheetUI(View view) {
            TextView subtitle = view.findViewById(R.id.tv_delete_subtitle);
            String detail = (med.dosage == null || med.dosage.isEmpty()) ? med.frequency
                    : med.dosage + " · " + med.frequency;
            String full = "Are you sure you want to remove " + med.name
                    + " (" + detail + ") from your schedule? This action cannot be undone.";
            SpannableString span = new SpannableString(full);

            int ns = full.indexOf(med.name);
            if (ns != -1) {
                int ne = ns + med.name.length();
                span.setSpan(new StyleSpan(Typeface.BOLD), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.Ebony)),
                        ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            subtitle.setText(span);

            ((TextView) view.findViewById(R.id.tv_med_name_card)).setText(med.name);
            ((TextView) view.findViewById(R.id.tv_med_detail_card)).setText(detail);

            view.findViewById(R.id.btn_confirm_delete).setOnClickListener(v -> {
                AppDataBase db = AppDataBase.getInstance(requireContext());
                new Thread(() -> {
                    ReminderScheduler.cancelAlarm(requireContext(), med.name, med.time);
                    db.medDAO().delete(med);

                    // 2. CHECK LIFECYCLE BEFORE TOUCHING UI
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            // 3. Safe to update UI
                            dismiss();
                            if (onConfirmed != null)
                                onConfirmed.run();
                        });
                    }
                }).start();
            });

            view.findViewById(R.id.btn_cancel_delete).setOnClickListener(v -> dismiss());
        }
    }

    // ─── Log Now flow ─────────────────────────────────────────────────────────

    private void onLogNowClicked(MedEntity med) {
        AppDataBase db = AppDataBase.getInstance(requireContext());
        new Thread(() -> {
            db.medDAO().incrementDaysTaken(med.id_meds);
            // 2. CHECK LIFECYCLE BEFORE TOUCHING UI
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    // 3. Safe to update UI
                    loadMedicines();
                });
            }
        }).start();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RecyclerView Adapter
    // ═════════════════════════════════════════════════════════════════════════

    public static class MedAdapter extends RecyclerView.Adapter<MedAdapter.MedViewHolder> {

        public interface OnDeleteListener {
            void onDelete(MedEntity med);
        }

        public interface OnLogNowListener {
            void onLogNow(MedEntity med);
        }

        public interface OnEditListener {
            void onEdit(MedEntity med);
        }

        private List<MedEntity> medList;
        private final OnDeleteListener deleteListener;
        private final OnLogNowListener logNowListener;
        private final OnEditListener editListener;

        public MedAdapter(List<MedEntity> medList,
                OnDeleteListener deleteListener,
                OnLogNowListener logNowListener,
                OnEditListener editListener) {
            this.medList = medList;
            this.deleteListener = deleteListener;
            this.logNowListener = logNowListener;
            this.editListener = editListener;
        }

        public void updateList(List<MedEntity> newList) {
            this.medList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_medicine_card, parent, false);
            return new MedViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MedViewHolder holder, int position) {
            holder.bind(medList.get(position), deleteListener, logNowListener, editListener);
        }

        @Override
        public int getItemCount() {
            return medList == null ? 0 : medList.size();
        }

        static class MedViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvTime, tvStatus, tvProgress, tvDayCount, tvNextDose;
            LinearProgressIndicator progressBar;
            View btnEdit, btnDelete, btnLogNow;

            MedViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_med_name);
                tvTime = itemView.findViewById(R.id.tv_med_time);
                tvStatus = itemView.findViewById(R.id.tv_med_status);
                tvProgress = itemView.findViewById(R.id.tv_progress_label);
                tvDayCount = itemView.findViewById(R.id.tv_day_count);
                tvNextDose = itemView.findViewById(R.id.tv_next_dose);
                progressBar = itemView.findViewById(R.id.progress_bar);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
                btnLogNow = itemView.findViewById(R.id.btn_log_now);
            }

            void bind(MedEntity med, OnDeleteListener dl, OnLogNowListener ll, OnEditListener el) {
                tvName.setText(med.name);
                tvTime.setText(med.time);

                if (med.isContinuous)
                    tvStatus.setText("Ongoing");
                else if (med.daysTaken >= med.durationDays)
                    tvStatus.setText("Completed");
                else
                    tvStatus.setText("Upcoming");

                if (med.isContinuous) {
                    progressBar.setProgress(100);
                    tvDayCount.setText("Continuous");
                } else {
                    int pct = med.durationDays > 0
                            ? (int) ((med.daysTaken / (float) med.durationDays) * 100)
                            : 0;
                    progressBar.setProgress(pct);
                    tvDayCount.setText("Day " + med.daysTaken + " of " + med.durationDays);
                }

                if (tvNextDose != null) {
                    String freqStr = "";
                    if (med.interval != null && !med.interval.equals("Daily")) {
                        freqStr = " (" + med.frequency + " x " + med.interval.replace("ly", "") + ")";
                    }
                    tvNextDose.setText("Next dose: " + med.time + freqStr);
                }

                if (btnLogNow != null) {
                    boolean canLog = med.isContinuous || med.daysTaken < med.durationDays;
                    btnLogNow.setVisibility(canLog ? View.VISIBLE : View.GONE);
                    btnLogNow.setOnClickListener(v -> ll.onLogNow(med));
                }
                if (btnEdit != null)
                    btnEdit.setOnClickListener(v -> el.onEdit(med));
                if (btnDelete != null)
                    btnDelete.setOnClickListener(v -> dl.onDelete(med));
            }
        }
    }
}