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

import com.example.do4e.navigation.FragmentNavigation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
    private View emptyState; // the meds_no fragment container (we show/hide it)

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_medicines, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Back button
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (requireActivity() instanceof FragmentNavigation) {
                ((FragmentNavigation) requireActivity()).popBackStack();
            }
        });

        // FAB → navigate to add_meds
        view.findViewById(R.id.fab_add).setOnClickListener(v -> {
            if (requireActivity() instanceof FragmentNavigation) {
                ((FragmentNavigation) requireActivity()).navigateTo(new add_meds(), true);
            }
        });

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
        loadMedicines(); // refresh when returning to this screen
    }

    // ─── Load from DB ─────────────────────────────────────────────────────────

    private void loadMedicines() {
        AppDataBase db = AppDataBase.getInstance(requireContext());
        new Thread(() -> {
            List<MedEntity> meds = db.medDAO().getAllMeds();
            requireActivity().runOnUiThread(() -> {
                if (adapter != null)
                    adapter.updateList(meds);
                updateEmptyState(meds.isEmpty());
            });
        }).start();
    }

    private void updateEmptyState(boolean isEmpty) {
        // The fragment layout has a scroll view with medicine cards.
        // If empty, navigate to the meds_no fragment instead.
        if (isEmpty) {
            if (requireActivity() instanceof FragmentNavigation) {
                ((FragmentNavigation) requireActivity()).replaceFragment(new meds_no());
            }
        }
    }

    // ─── Delete flow ──────────────────────────────────────────────────────────

    private void onDeleteClicked(MedEntity med) {
        BottomSheetDialogFragment sheet = new BottomSheetDialogFragment() {
            @Nullable
            @Override
            public View onCreateView(@NonNull LayoutInflater inflater,
                    @Nullable ViewGroup container,
                    @Nullable Bundle savedInstanceState) {
                return inflater.inflate(R.layout.bottom_sheet_delete, container, false);
            }

            @Override
            public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
                        // Cancel the alarm first
                        ReminderScheduler.cancelAlarm(requireContext(), med.name, med.time);
                        db.medDAO().delete(med);
                        requireActivity().runOnUiThread(() -> {
                            dismiss();
                            loadMedicines(); // refresh list
                        });
                    }).start();
                });

                view.findViewById(R.id.btn_cancel_delete).setOnClickListener(v -> dismiss());
            }
        };
        sheet.show(getParentFragmentManager(), "delete_sheet");
    }

    // ─── Log Now flow ─────────────────────────────────────────────────────────

    private void onLogNowClicked(MedEntity med) {
        AppDataBase db = AppDataBase.getInstance(requireContext());
        new Thread(() -> {
            db.medDAO().incrementDaysTaken(med.id_meds);
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
            MedEntity med = medList.get(position);
            holder.bind(med, deleteListener, logNowListener);
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

            void bind(MedEntity med,
                    OnDeleteListener deleteListener,
                    OnLogNowListener logNowListener) {
                tvName.setText(med.name);
                tvTime.setText(med.time);

                // Status badge
                if (med.isContinuous) {
                    tvStatus.setText("Ongoing");
                } else if (med.daysTaken >= med.durationDays) {
                    tvStatus.setText("Completed");
                } else {
                    tvStatus.setText("Upcoming");
                }

                // Progress bar
                if (med.isContinuous) {
                    progressBar.setProgress(100);
                    tvDayCount.setText("Continuous");
                } else {
                    int progress = med.durationDays > 0
                            ? (int) ((med.daysTaken / (float) med.durationDays) * 100)
                            : 0;
                    progressBar.setProgress(progress);
                    tvDayCount.setText("Day " + med.daysTaken + " of " + med.durationDays);
                }

                // Next dose hint
                if (tvNextDose != null)
                    tvNextDose.setText("Next dose: " + med.time);

                // Log Now visibility — only show if not yet completed
                if (btnLogNow != null) {
                    boolean canLog = med.isContinuous || med.daysTaken < med.durationDays;
                    btnLogNow.setVisibility(canLog ? View.VISIBLE : View.GONE);
                    btnLogNow.setOnClickListener(v -> logNowListener.onLogNow(med));
                }

                // Edit (placeholder — can wire later)
                if (btnEdit != null)
                    btnEdit.setOnClickListener(v -> {
                        /* TODO: edit */ });

                // Delete
                if (btnDelete != null)
                    btnDelete.setOnClickListener(v -> deleteListener.onDelete(med));
            }
        }
    }
}