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
import androidx.navigation.NavController;
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
    private View emptyStateLayout;
    private View medicinesListLayout;
    private View fabAdd;
    private View btnAddMedicine;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_medicines, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Cache views
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        medicinesListLayout = view.findViewById(R.id.medicines_list_layout);
        fabAdd = view.findViewById(R.id.fab_add);
        btnAddMedicine = view.findViewById(R.id.btn_add_medicine);

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
            adapter = new MedAdapter(new ArrayList<>(), this::onDeleteClicked, this::onLogNowClicked);
            recyclerView.setAdapter(adapter);
        }

        loadMedicines();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMedicines();
    }

    // ─── Load from DB ─────────────────────────────────────────────────────────

    private void loadMedicines() {
        AppDataBase db = AppDataBase.getInstance(requireContext());
        new Thread(() -> {
            List<MedEntity> meds = db.medDAO().getAllMeds();
            if (!isAdded())
                return; // guard: fragment may be detached
            requireActivity().runOnUiThread(() -> {
                if (adapter != null)
                    adapter.updateList(meds);
                updateEmptyState(meds.isEmpty());
            });
        }).start();
    }

    /**
     * Toggles between the empty state and the list state
     * purely via visibility — no fragment navigation, no blink.
     */
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            btnAddMedicine.setVisibility(View.VISIBLE);
            medicinesListLayout.setVisibility(View.GONE);
            fabAdd.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            btnAddMedicine.setVisibility(View.GONE);
            medicinesListLayout.setVisibility(View.VISIBLE);
            fabAdd.setVisibility(View.VISIBLE);
        }
    }

    // ─── Delete flow ──────────────────────────────────────────────────────────

    private void onDeleteClicked(MedEntity med) {
        DeleteSheet sheet = DeleteSheet.newInstance(med);
        sheet.setOnDeleteConfirmed(() -> loadMedicines());
        sheet.show(getParentFragmentManager(), "delete_sheet");
    }

    /** Must be public static so Android can recreate it from instance state. */
    public static class DeleteSheet extends com.google.android.material.bottomsheet.BottomSheetDialogFragment {

        private Runnable onConfirmed;

        // We pass only primitives through the Bundle; hold the MedEntity via a static
        // pass-through since it's not Parcelable. We use a simple holder approach.
        private MedEntity med;

        public static DeleteSheet newInstance(MedEntity med) {
            DeleteSheet sheet = new DeleteSheet();
            sheet.med = med; // held in memory; fine since no process-death scenario here
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
            if (med == null) {
                dismiss();
                return;
            }

            // Build subtitle with bold med name
            TextView subtitle = view.findViewById(R.id.tv_delete_subtitle);
            String detail = med.dosage.isEmpty() ? med.frequency : med.dosage + " · " + med.frequency;
            String full = "Are you sure you want to remove " + med.name
                    + " (" + detail + ") from your schedule? This action cannot be undone.";
            SpannableString span = new SpannableString(full);
            int ns = full.indexOf(med.name), ne = ns + med.name.length();
            span.setSpan(new StyleSpan(Typeface.BOLD), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(
                    ContextCompat.getColor(requireContext(), R.color.Ebony)),
                    ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            subtitle.setText(span);

            // Medicine info card
            ((TextView) view.findViewById(R.id.tv_med_name_card)).setText(med.name);
            ((TextView) view.findViewById(R.id.tv_med_detail_card)).setText(detail);

            // Confirm delete
            view.findViewById(R.id.btn_confirm_delete).setOnClickListener(v -> {
                AppDataBase db = AppDataBase.getInstance(requireContext());
                new Thread(() -> {
                    ReminderScheduler.cancelAlarm(requireContext(), med.name, med.time);
                    db.medDAO().delete(med);
                    if (!isAdded())
                        return;
                    requireActivity().runOnUiThread(() -> {
                        dismiss();
                        if (onConfirmed != null)
                            onConfirmed.run();
                    });
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
            if (!isAdded())
                return;
            requireActivity().runOnUiThread(this::loadMedicines);
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

        private List<MedEntity> medList;
        private final OnDeleteListener deleteListener;
        private final OnLogNowListener logNowListener;

        public MedAdapter(List<MedEntity> medList,
                OnDeleteListener deleteListener,
                OnLogNowListener logNowListener) {
            this.medList = medList;
            this.deleteListener = deleteListener;
            this.logNowListener = logNowListener;
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
            holder.bind(medList.get(position), deleteListener, logNowListener);
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

            void bind(MedEntity med, OnDeleteListener dl, OnLogNowListener ll) {
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

                if (tvNextDose != null)
                    tvNextDose.setText("Next dose: " + med.time);

                if (btnLogNow != null) {
                    boolean canLog = med.isContinuous || med.daysTaken < med.durationDays;
                    btnLogNow.setVisibility(canLog ? View.VISIBLE : View.GONE);
                    btnLogNow.setOnClickListener(v -> ll.onLogNow(med));
                }
                if (btnEdit != null)
                    btnEdit.setOnClickListener(v -> {
                        /* TODO */ });
                if (btnDelete != null)
                    btnDelete.setOnClickListener(v -> dl.onDelete(med));
            }
        }
    }
}