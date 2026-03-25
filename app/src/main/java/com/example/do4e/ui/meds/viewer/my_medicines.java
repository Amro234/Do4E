package com.example.do4e.ui.meds.viewer;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do4e.core.utility.ClickSoundHelper;
import com.example.do4e.R;
import com.example.do4e.data.repository.MedRepository;
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.ReminderScheduler;
import com.example.do4e.ui.meds.presenter.MyMedicinesContract;
import com.example.do4e.ui.meds.presenter.MyMedicinesPresenter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class my_medicines extends Fragment implements MyMedicinesContract.View {

    private MyMedicinesContract.Presenter presenter;

    private MedAdapter adapterToday, adapterDaily, adapterWeekly, adapterMonthly;
    private List<MedEntity> todaysMeds = new ArrayList<>();
    private List<MedEntity> dailyMeds = new ArrayList<>();
    private List<MedEntity> weeklyMeds = new ArrayList<>();
    private List<MedEntity> monthlyMeds = new ArrayList<>();

    private ViewPager2 viewPager;
    private TextView tabToday, tabDaily, tabWeekly, tabMonthly;
    private View fabAdd;

    private String currentTab = "Today";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_medicines, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter = new MyMedicinesPresenter(this, requireContext());

        if (getArguments() != null) {
            String startTab = getArguments().getString("start_interval");
            if (startTab != null) currentTab = startTab;
        }

        view.findViewById(R.id.btn_back).setOnClickListener(ClickSoundHelper.get(requireContext()).wrap(v -> NavHostFragment.findNavController(this).popBackStack()));

        fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(ClickSoundHelper.get(requireContext()).wrap(v -> NavHostFragment.findNavController(this).navigate(R.id.action_my_medicines_to_add_meds)));

        tabToday = view.findViewById(R.id.tab_today);
        tabDaily = view.findViewById(R.id.tab_daily);
        tabWeekly = view.findViewById(R.id.tab_weekly);
        tabMonthly = view.findViewById(R.id.tab_monthly);

        tabToday.setOnClickListener(v -> { ClickSoundHelper.get(requireContext()).playClick(); viewPager.setCurrentItem(0, true); });
        tabDaily.setOnClickListener(v -> { ClickSoundHelper.get(requireContext()).playClick(); viewPager.setCurrentItem(1, true); });
        tabWeekly.setOnClickListener(v -> { ClickSoundHelper.get(requireContext()).playClick(); viewPager.setCurrentItem(2, true); });
        tabMonthly.setOnClickListener(v -> { ClickSoundHelper.get(requireContext()).playClick(); viewPager.setCurrentItem(3, true); });

        viewPager = view.findViewById(R.id.view_pager);
        viewPager.setAdapter(new TabPagerAdapter());
        viewPager.setOffscreenPageLimit(3);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                String[] tabs = { "Today", "Daily", "Weekly", "Monthly" };
                currentTab = tabs[position];
                updateTabStyles();
                updateFabVisibilityInternal();
            }
        });

        viewPager.setCurrentItem(tabIndexFor(currentTab), false);
        updateTabStyles();
        presenter.loadMedicines();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.loadMedicines();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (presenter instanceof MyMedicinesPresenter) {
            ((MyMedicinesPresenter) presenter).dispose();
        }
    }

    @Override
    public void refreshTabs(List<MedEntity> todaysMeds, List<MedEntity> dailyMeds, List<MedEntity> weeklyMeds, List<MedEntity> monthlyMeds) {
        this.todaysMeds = todaysMeds;
        this.dailyMeds = dailyMeds;
        this.weeklyMeds = weeklyMeds;
        this.monthlyMeds = monthlyMeds;

        if (adapterToday != null) adapterToday.updateList(todaysMeds);
        if (adapterDaily != null) adapterDaily.updateList(dailyMeds);
        if (adapterWeekly != null) adapterWeekly.updateList(weeklyMeds);
        if (adapterMonthly != null) adapterMonthly.updateList(monthlyMeds);

        updateFabVisibilityInternal();
    }

    @Override
    public void updateFabVisibility(boolean hasMeds) {
        // Handled internally
    }

    private void updateFabVisibilityInternal() {
        boolean hasMeds = presenter != null && presenter.hasMedsForTab(currentTab);
        if (fabAdd != null) fabAdd.setVisibility(hasMeds ? View.VISIBLE : View.GONE);
    }

    private class TabPagerAdapter extends RecyclerView.Adapter<TabPagerAdapter.PageHolder> {
        final String[] tabNames = { "Today", "Daily", "Weekly", "Monthly" };

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView rv = new RecyclerView(parent.getContext());
            rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            rv.setLayoutManager(new LinearLayoutManager(parent.getContext()));
            rv.setPadding(0, 0, 0, 80);
            rv.setClipToPadding(false);
            return new PageHolder(rv);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            MedAdapter adapter = new MedAdapter(
                    new ArrayList<>(),
                    tabNames[position],
                    my_medicines.this::onDeleteClicked,
                    my_medicines.this::onLogNowClicked,
                    my_medicines.this::onEditClicked,
                    () -> fabAdd.performClick());
            holder.recyclerView.setAdapter(adapter);

            switch (position) {
                case 0: adapterToday = adapter; adapter.updateList(todaysMeds); break;
                case 1: adapterDaily = adapter; adapter.updateList(dailyMeds); break;
                case 2: adapterWeekly = adapter; adapter.updateList(weeklyMeds); break;
                case 3: adapterMonthly = adapter; adapter.updateList(monthlyMeds); break;
            }
        }

        @Override
        public int getItemCount() { return 4; }

        class PageHolder extends RecyclerView.ViewHolder {
            RecyclerView recyclerView;
            PageHolder(RecyclerView rv) { super(rv); recyclerView = rv; }
        }
    }

    private int tabIndexFor(String tab) {
        switch (tab) {
            case "Daily": return 1;
            case "Weekly": return 2;
            case "Monthly": return 3;
            default: return 0;
        }
    }

    private void updateTabStyles() {
        if (getContext() == null) return;
        TextView[] tabs = { tabToday, tabDaily, tabWeekly, tabMonthly };
        String[] names = { "Today", "Daily", "Weekly", "Monthly" };
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = names[i].equals(currentTab);
            tabs[i].setBackgroundResource(selected ? R.drawable.bg_tab_selected : 0);
            tabs[i].setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.junglegreen : R.color.StateGray));
        }
    }

    private void onEditClicked(MedEntity med) {
        Bundle args = new Bundle();
        args.putInt("med_id", med.id_meds);
        NavHostFragment.findNavController(this).navigate(R.id.action_my_medicines_to_add_meds, args);
    }

    private void onDeleteClicked(MedEntity med) {
        DeleteSheet sheet = DeleteSheet.newInstance(med.id_meds);
        sheet.setOnDeleteConfirmed(() -> presenter.loadMedicines());
        sheet.show(getParentFragmentManager(), "delete_sheet");
    }

    private void onLogNowClicked(MedEntity med) {
        presenter.logNowClicked(med);
    }

    public static class DeleteSheet extends BottomSheetDialogFragment {
        private Runnable onConfirmed;
        private MedEntity med;
        private final io.reactivex.rxjava3.disposables.CompositeDisposable disposables = new io.reactivex.rxjava3.disposables.CompositeDisposable();

        public static DeleteSheet newInstance(int medId) {
            DeleteSheet sheet = new DeleteSheet();
            Bundle args = new Bundle();
            args.putInt("MED_ID", medId);
            sheet.setArguments(args);
            return sheet;
        }

        public void setOnDeleteConfirmed(Runnable callback) { this.onConfirmed = callback; }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.bottom_sheet_delete, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            if (getArguments() == null) { dismiss(); return; }

            int medId = getArguments().getInt("MED_ID");
            MedRepository repo = MedRepository.getInstance(requireContext());

            disposables.add(
                repo.getMedicineById(medId)
                    .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                    .subscribe(medResult -> {
                        this.med = medResult;
                        if (med == null) { dismiss(); return; }
                        setupSheetUI(view, repo);
                    }, throwable -> dismiss())
            );
        }

        private void setupSheetUI(View view, MedRepository repo) {
            TextView subtitle = view.findViewById(R.id.tv_delete_subtitle);
            String detail = (med.dosage == null || med.dosage.isEmpty()) ? med.frequency : med.dosage + " · " + med.frequency;
            String full = "Are you sure you want to remove " + med.name + " (" + detail + ") from your schedule? This action cannot be undone.";
            SpannableString span = new SpannableString(full);
            int ns = full.indexOf(med.name);
            if (ns != -1) {
                int ne = ns + med.name.length();
                span.setSpan(new StyleSpan(Typeface.BOLD), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.Ebony)), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            subtitle.setText(span);

            ((TextView) view.findViewById(R.id.tv_med_name_card)).setText(med.name);
            ((TextView) view.findViewById(R.id.tv_med_detail_card)).setText(detail);

            view.findViewById(R.id.btn_confirm_delete).setOnClickListener(v -> {
                ClickSoundHelper.get(requireContext()).playClick();
                ReminderScheduler.cancelAlarm(requireContext(), med.name, med.time);
                disposables.add(
                    repo.deleteMedicine(med)
                        .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                            dismiss();
                            if (onConfirmed != null) onConfirmed.run();
                        }, throwable -> dismiss())
                );
            });

            view.findViewById(R.id.btn_cancel_delete).setOnClickListener(v -> {
                ClickSoundHelper.get(requireContext()).playClick();
                dismiss();
            });
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            disposables.clear();
        }
    }

    public static class MedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_EMPTY = 1;

        public interface OnDeleteListener { void onDelete(MedEntity med); }
        public interface OnLogNowListener { void onLogNow(MedEntity med); }
        public interface OnEditListener { void onEdit(MedEntity med); }
        public interface OnAddListener { void onAdd(); }

        private List<MedEntity> medList;
        private final String tabName;
        private final OnDeleteListener deleteListener;
        private final OnLogNowListener logNowListener;
        private final OnEditListener editListener;
        private final OnAddListener addListener;

        public MedAdapter(List<MedEntity> medList, String tabName, OnDeleteListener deleteListener, OnLogNowListener logNowListener, OnEditListener editListener, OnAddListener addListener) {
            this.medList = medList; this.tabName = tabName; this.deleteListener = deleteListener; this.logNowListener = logNowListener; this.editListener = editListener; this.addListener = addListener;
        }

        public void updateList(List<MedEntity> newList) { this.medList = newList; notifyDataSetChanged(); }

        @Override
        public int getItemViewType(int position) { return (medList == null || medList.isEmpty()) ? TYPE_EMPTY : TYPE_ITEM; }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_EMPTY) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.medicine_no, parent, false);
                v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return new EmptyViewHolder(v);
            }
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine_card, parent, false);
            return new MedViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof EmptyViewHolder) ((EmptyViewHolder) holder).bind(tabName, addListener);
            else if (holder instanceof MedViewHolder) ((MedViewHolder) holder).bind(medList.get(position), deleteListener, logNowListener, editListener);
        }

        @Override
        public int getItemCount() { return (medList == null || medList.isEmpty()) ? 1 : medList.size(); }

        static class EmptyViewHolder extends RecyclerView.ViewHolder {
            TextView title, sub; View btnAdd;
            EmptyViewHolder(@NonNull View itemView) {
                super(itemView); title = itemView.findViewById(R.id.tv_no_medicine_title); sub = itemView.findViewById(R.id.tv_no_medicine_sub); btnAdd = itemView.findViewById(R.id.btn_add_medicine);
            }
            void bind(String tab, OnAddListener listener) {
                btnAdd.setOnClickListener(v -> { ClickSoundHelper.get(itemView.getContext()).playClick(); listener.onAdd(); });
                switch (tab) {
                    case "Daily": title.setText(itemView.getContext().getString(R.string.Medicine_No_Daily)); sub.setText("Add your first daily medicine to get started."); break;
                    case "Weekly": title.setText(itemView.getContext().getString(R.string.Medicine_NO_Weekly)); sub.setText("Add your first weekly medicine to get started."); break;
                    case "Monthly": title.setText(itemView.getContext().getString(R.string.Medicine_No_Monthly)); sub.setText("Add your first monthly medicine to get started."); break;
                    default: title.setText(itemView.getContext().getString(R.string.Medicine_No)); sub.setText("Add your first medicine to get started."); break;
                }
            }
        }

        static class MedViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvTime, tvStatus, tvProgress, tvDayCount, tvNextDose, tvStartDate; ImageView ivMedType; LinearProgressIndicator progressBar; View btnEdit, btnDelete, btnLogNow;
            MedViewHolder(@NonNull View itemView) {
                super(itemView); tvName = itemView.findViewById(R.id.tv_med_name); tvTime = itemView.findViewById(R.id.tv_med_time); tvStatus = itemView.findViewById(R.id.tv_med_status); tvProgress = itemView.findViewById(R.id.tv_progress_label); tvDayCount = itemView.findViewById(R.id.tv_day_count); tvNextDose = itemView.findViewById(R.id.tv_next_dose); tvStartDate = itemView.findViewById(R.id.tv_start_date); ivMedType = itemView.findViewById(R.id.iv_med_type); progressBar = itemView.findViewById(R.id.progress_bar); btnEdit = itemView.findViewById(R.id.btn_edit); btnDelete = itemView.findViewById(R.id.btn_delete); btnLogNow = itemView.findViewById(R.id.btn_log_now);
            }
            void bind(MedEntity med, OnDeleteListener dl, OnLogNowListener ll, OnEditListener el) {
                tvName.setText(med.name); tvTime.setText(med.time);
                if (ivMedType != null) {
                    if ("Syrup".equals(med.medType)) ivMedType.setImageResource(R.drawable.serup_24dp_icon);
                    else if ("Syringe".equals(med.medType)) ivMedType.setImageResource(R.drawable.syringe_24dp_icon);
                    else ivMedType.setImageResource(R.drawable.pill_24dp_icon);
                }
                if (tvStartDate != null) {
                    if (med.startDate != 0) tvStartDate.setText(new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date(med.startDate)));
                    else tvStartDate.setText("Today");
                }
                if (med.isContinuous) tvStatus.setText("Ongoing"); else if (med.daysTaken >= med.durationDays) tvStatus.setText("Completed"); else tvStatus.setText("Upcoming");
                if (med.isContinuous) { progressBar.setProgress(100); tvDayCount.setText("Continuous"); }
                else { progressBar.setProgress(med.durationDays > 0 ? (int) ((med.daysTaken / (float) med.durationDays) * 100) : 0); tvDayCount.setText("Day " + med.daysTaken + " of " + med.durationDays); }
                if (tvNextDose != null) { String freqStr = (med.interval != null && !med.interval.equals("Daily")) ? " (" + med.frequency + " x " + med.interval.replace("ly", "") + ")" : ""; tvNextDose.setText("Next dose: " + med.time + freqStr); }
                if (btnLogNow != null) { boolean canLog = med.isContinuous || med.daysTaken < med.durationDays; btnLogNow.setVisibility(canLog ? View.VISIBLE : View.GONE); btnLogNow.setOnClickListener(v -> { ClickSoundHelper.get(v.getContext()).playClick(); ll.onLogNow(med); }); }
                if (btnEdit != null) btnEdit.setOnClickListener(v -> { ClickSoundHelper.get(v.getContext()).playClick(); el.onEdit(med); });
                if (btnDelete != null) btnDelete.setOnClickListener(v -> { ClickSoundHelper.get(v.getContext()).playClick(); dl.onDelete(med); });
            }
        }
    }
}