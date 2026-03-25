package com.example.do4e.ui.meds.viewer;

import android.app.TimePickerDialog;
import android.graphics.Color;
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
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.do4e.core.utility.ClickSoundHelper;
import com.example.do4e.R;
import com.example.do4e.db.MedEntity;
import com.example.do4e.ui.meds.presenter.AddMedsContract;
import com.example.do4e.ui.meds.presenter.AddMedsPresenter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class add_meds extends Fragment implements AddMedsContract.View {

    private AddMedsContract.Presenter presenter;

    private int selectedMedId = -1;
    private int selectedHour = 8;
    private int selectedMinute = 30;
    private String interval = "Daily";
    private String frequency = "1 time";
    private String medType = "Pill";
    private String instruction = "Before Food";
    private int daysCount = 1;
    private boolean isContinuous = false;
    private long selectedStartDate = System.currentTimeMillis();
    private TextView tvStartDate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_meds, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter = new AddMedsPresenter(this, requireContext());

        tvStartDate = view.findViewById(R.id.tv_start_date_value);
        tvStartDate.setText("Today (" + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(selectedStartDate)) + ")");

        view.findViewById(R.id.date_selection_container).setOnClickListener(v -> {
            ClickSoundHelper.get(requireContext()).playClick();
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Start Date").setSelection(selectedStartDate).build();
            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedStartDate = selection;
                tvStartDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(selection)));
            });
            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        view.findViewById(R.id.btn_back).setOnClickListener(ClickSoundHelper.get(requireContext()).wrap(v -> NavHostFragment.findNavController(this).popBackStack()));

        View.OnClickListener timeListener = v -> {
            ClickSoundHelper.get(requireContext()).playClick();
            new TimePickerDialog(requireContext(), (picker, h, m) -> {
                selectedHour = h;
                selectedMinute = m;
                updateTimeUI(view);
            }, selectedHour, selectedMinute, false).show();
        };

        view.findViewById(R.id.tv_selected_hour).setOnClickListener(timeListener);
        view.findViewById(R.id.tv_selected_minute).setOnClickListener(timeListener);
        ((View) view.findViewById(R.id.tv_selected_hour).getParent()).setOnClickListener(timeListener);
        ((View) view.findViewById(R.id.tv_selected_minute).getParent()).setOnClickListener(timeListener);

        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_am_pm)).addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateAmPmToggleStyle(view, checkedId);
        });
        updateAmPmToggleStyle(view, R.id.btn_am);

        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_interval)).addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            TextView tvFreqLabel = view.findViewById(R.id.tv_freq_label);
            TextView tvUnit = view.findViewById(R.id.tv_duration_unit);
            if (checkedId == R.id.btn_daily) { interval = "Daily"; tvFreqLabel.setText("Times per Day"); tvUnit.setText("DAYS"); }
            else if (checkedId == R.id.btn_weekly_interval) { interval = "Weekly"; tvFreqLabel.setText("Times per Week"); tvUnit.setText("WEEKS"); }
            else if (checkedId == R.id.btn_monthly_interval) { interval = "Monthly"; tvFreqLabel.setText("Times per Month"); tvUnit.setText("MONTHS"); }
        });

        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_frequency)).addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_once) frequency = "1 time";
            else if (checkedId == R.id.btn_twice) frequency = "2 times";
            else if (checkedId == R.id.btn_thrice) frequency = "3 times";
        });

        MaterialButton btnPill = view.findViewById(R.id.btn_pill);
        MaterialButton btnSyrup = view.findViewById(R.id.btn_syrup);
        MaterialButton btnSyringe = view.findViewById(R.id.btn_syringe);

        View.OnClickListener typeListener = v -> {
            ClickSoundHelper.get(requireContext()).playClick();
            setTypeButtonUnselected(btnPill); setTypeButtonUnselected(btnSyrup); setTypeButtonUnselected(btnSyringe);
            setTypeButtonSelected((MaterialButton) v);
            if (v.getId() == R.id.btn_pill) medType = "Pill";
            else if (v.getId() == R.id.btn_syrup) medType = "Syrup";
            else if (v.getId() == R.id.btn_syringe) medType = "Syringe";
        };
        btnPill.setOnClickListener(typeListener); btnSyrup.setOnClickListener(typeListener); btnSyringe.setOnClickListener(typeListener);

        MaterialButton btnBefore = view.findViewById(R.id.btn_before_food);
        MaterialButton btnDuring = view.findViewById(R.id.btn_during_food);
        MaterialButton btnAfter = view.findViewById(R.id.btn_after_food);

        View.OnClickListener foodListener = v -> {
            ClickSoundHelper.get(requireContext()).playClick();
            setTypeButtonUnselected(btnBefore); setTypeButtonUnselected(btnDuring); setTypeButtonUnselected(btnAfter);
            setTypeButtonSelected((MaterialButton) v);
            if (v.getId() == R.id.btn_before_food) instruction = "Before Food";
            else if (v.getId() == R.id.btn_during_food) instruction = "During Food";
            else if (v.getId() == R.id.btn_after_food) instruction = "After Food";
        };
        btnBefore.setOnClickListener(foodListener); btnDuring.setOnClickListener(foodListener); btnAfter.setOnClickListener(foodListener);

        TextView tvDays = view.findViewById(R.id.tv_days_value);
        view.findViewById(R.id.btn_days_plus).setOnClickListener(v -> { ClickSoundHelper.get(requireContext()).playClick(); daysCount++; tvDays.setText(String.valueOf(daysCount)); });
        view.findViewById(R.id.btn_days_minus).setOnClickListener(v -> { ClickSoundHelper.get(requireContext()).playClick(); if (daysCount > 1) { daysCount--; tvDays.setText(String.valueOf(daysCount)); } });

        SwitchMaterial switchContinuous = view.findViewById(R.id.switch_continuous);
        switchContinuous.setOnCheckedChangeListener((btn, checked) -> {
            isContinuous = checked;
            view.findViewById(R.id.layout_days_input).setVisibility(checked ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.layout_continuous_badge).setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        view.findViewById(R.id.btn_reset).setOnClickListener(ClickSoundHelper.get(requireContext()).wrap(v -> resetForm(view)));
        view.findViewById(R.id.btn_save).setOnClickListener(ClickSoundHelper.get(requireContext()).wrap(v -> extractAndSave(view)));

        if (getArguments() != null) {
            selectedMedId = getArguments().getInt("med_id", -1);
            if (selectedMedId != -1) {
                ((TextView) view.findViewById(R.id.add_meds_title_text)).setText("Edit Medicine");
                presenter.loadMedicine(selectedMedId);
            }
        }
    }

    private void updateTimeUI(View view) {
        TextView tvHour = view.findViewById(R.id.tv_selected_hour);
        TextView tvMinute = view.findViewById(R.id.tv_selected_minute);
        int h12 = selectedHour % 12;
        if (h12 == 0) h12 = 12;
        tvHour.setText(String.format(Locale.getDefault(), "%02d", h12));
        tvMinute.setText(String.format(Locale.getDefault(), "%02d", selectedMinute));
        MaterialButtonToggleGroup toggleAmPm = view.findViewById(R.id.toggle_am_pm);
        toggleAmPm.check(selectedHour >= 12 ? R.id.btn_pm : R.id.btn_am);
    }

    private void extractAndSave(View view) {
        TextInputLayout nameLayout = view.findViewById(R.id.name_meds);
        TextInputEditText etName = (TextInputEditText) nameLayout.getEditText();
        String name = etName != null ? etName.getText().toString().trim() : "";

        TextInputLayout dosageLayout = view.findViewById(R.id.dosage_med);
        TextInputEditText etDosage = (TextInputEditText) dosageLayout.getEditText();
        String dosage = etDosage != null ? etDosage.getText().toString().trim() : "";

        TextInputLayout notesLayout = view.findViewById(R.id.notes_med);
        TextInputEditText etNotes = (TextInputEditText) notesLayout.getEditText();
        String notes = etNotes != null ? etNotes.getText().toString().trim() : "";

        presenter.saveMedicine(name, dosage, notes, selectedHour, selectedMinute, interval, frequency, daysCount, isContinuous, medType, instruction, selectedStartDate, selectedMedId);
    }

    @Override
    public void populateForm(MedEntity med) {
        View view = getView();
        if (view == null) return;

        TextInputLayout nameLayout = view.findViewById(R.id.name_meds);
        if (nameLayout.getEditText() != null) nameLayout.getEditText().setText(med.name);

        TextInputLayout dosageLayout = view.findViewById(R.id.dosage_med);
        if (dosageLayout.getEditText() != null) dosageLayout.getEditText().setText(med.dosage);

        selectedHour = med.hour; selectedMinute = med.minute;
        updateTimeUI(view);

        MaterialButtonToggleGroup toggleInterval = view.findViewById(R.id.toggle_interval);
        if (med.interval != null) {
            if (med.interval.equals("Daily")) toggleInterval.check(R.id.btn_daily);
            else if (med.interval.equals("Weekly")) toggleInterval.check(R.id.btn_weekly_interval);
            else if (med.interval.equals("Monthly")) toggleInterval.check(R.id.btn_monthly_interval);
        }

        MaterialButtonToggleGroup toggleFreq = view.findViewById(R.id.toggle_frequency);
        if ("1 time".equals(med.frequency)) toggleFreq.check(R.id.btn_once);
        else if ("2 times".equals(med.frequency)) toggleFreq.check(R.id.btn_twice);
        else if ("3 times".equals(med.frequency)) toggleFreq.check(R.id.btn_thrice);
        frequency = med.frequency;

        ((SwitchMaterial) view.findViewById(R.id.switch_continuous)).setChecked(med.isContinuous);
        if (!med.isContinuous) { daysCount = med.durationDays; ((TextView) view.findViewById(R.id.tv_days_value)).setText(String.valueOf(daysCount)); }

        selectedStartDate = med.startDate != 0 ? med.startDate : System.currentTimeMillis();
        tvStartDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(selectedStartDate)));

        MaterialButton btnPill = view.findViewById(R.id.btn_pill); MaterialButton btnSyrup = view.findViewById(R.id.btn_syrup); MaterialButton btnSyringe = view.findViewById(R.id.btn_syringe);
        setTypeButtonUnselected(btnPill); setTypeButtonUnselected(btnSyrup); setTypeButtonUnselected(btnSyringe);
        if ("Pill".equals(med.medType)) setTypeButtonSelected(btnPill);
        else if ("Syrup".equals(med.medType)) setTypeButtonSelected(btnSyrup);
        else if ("Syringe".equals(med.medType)) setTypeButtonSelected(btnSyringe);
        medType = med.medType;

        MaterialButton btnBefore = view.findViewById(R.id.btn_before_food); MaterialButton btnDuring = view.findViewById(R.id.btn_during_food); MaterialButton btnAfter = view.findViewById(R.id.btn_after_food);
        setTypeButtonUnselected(btnBefore); setTypeButtonUnselected(btnDuring); setTypeButtonUnselected(btnAfter);
        if ("Before Food".equals(med.instruction)) setTypeButtonSelected(btnBefore);
        else if ("During Food".equals(med.instruction)) setTypeButtonSelected(btnDuring);
        else if ("After Food".equals(med.instruction)) setTypeButtonSelected(btnAfter);
        instruction = med.instruction;

        TextInputLayout notesLayout = view.findViewById(R.id.notes_med);
        if (notesLayout.getEditText() != null) notesLayout.getEditText().setText(med.notes);
    }

    @Override
    public void showNameError(String error) {
        if (getView() == null) return;
        TextInputLayout nameLayout = getView().findViewById(R.id.name_meds);
        nameLayout.setError(error);
    }

    @Override
    public void showSuccessSheet(String medName, String timeStr, String intervalStr) {
        NavController navController = NavHostFragment.findNavController(this);
        SuccessSheet sheet = SuccessSheet.newInstance(medName, timeStr, intervalStr);
        sheet.setNavController(navController);
        sheet.show(getParentFragmentManager(), "success_sheet");
    }

    @Override
    public void showErrorToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    public static class SuccessSheet extends BottomSheetDialogFragment {
        private NavController navController;
        public static SuccessSheet newInstance(String medName, String time, String interval) {
            SuccessSheet sheet = new SuccessSheet(); Bundle args = new Bundle(); args.putString("medName", medName); args.putString("time", time); args.putString("interval", interval); sheet.setArguments(args); return sheet;
        }
        public void setNavController(NavController nc) { this.navController = nc; }
        @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) { return inflater.inflate(R.layout.bottom_sheet_success, container, false); }
        @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            String medName = requireArguments().getString("medName", ""); String time = requireArguments().getString("time", "");
            TextView subtitle = view.findViewById(R.id.tv_success_subtitle);
            String full = "We'll remind you to take " + medName + " at " + time + "."; SpannableString span = new SpannableString(full);
            int teal = ContextCompat.getColor(requireContext(), R.color.junglegreen);
            int ns = full.indexOf(medName), ne = ns + medName.length(); span.setSpan(new ForegroundColorSpan(teal), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); span.setSpan(new StyleSpan(Typeface.BOLD), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            int ts = full.lastIndexOf(time), te = ts + time.length(); if (ts >= 0) { span.setSpan(new ForegroundColorSpan(teal), ts, te, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); span.setSpan(new StyleSpan(Typeface.BOLD), ts, te, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); }
            subtitle.setText(span);
            view.findViewById(R.id.btn_back_to_home).setOnClickListener(v -> { ClickSoundHelper.get(requireContext()).playClick(); dismiss(); if (navController != null) navController.navigate(R.id.home_id); });
            view.findViewById(R.id.btn_view_schedule).setOnClickListener(v -> { ClickSoundHelper.get(requireContext()).playClick(); dismiss(); if (navController != null) navController.navigate(R.id.schedule); });
        }
    }

    private void resetForm(View view) {
        TextInputLayout nameLayout = view.findViewById(R.id.name_meds); if (nameLayout.getEditText() != null) nameLayout.getEditText().setText("");
        TextInputLayout dosageLayout = view.findViewById(R.id.dosage_med); if (dosageLayout.getEditText() != null) dosageLayout.getEditText().setText("");
        TextInputLayout notesLayout = view.findViewById(R.id.notes_med); if (notesLayout.getEditText() != null) notesLayout.getEditText().setText("");
        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_am_pm)).check(R.id.btn_am);
        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_interval)).check(R.id.btn_daily);
        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_frequency)).check(R.id.btn_once);
        selectedMedId = -1; interval = "Daily"; frequency = "1 time"; medType = "Pill"; instruction = "Before Food"; daysCount = 1; isContinuous = false;
        ((TextView) view.findViewById(R.id.tv_days_value)).setText("1");
        ((SwitchMaterial) view.findViewById(R.id.switch_continuous)).setChecked(false);
        view.findViewById(R.id.layout_days_input).setVisibility(View.VISIBLE); view.findViewById(R.id.layout_continuous_badge).setVisibility(View.GONE);
    }

    private void updateAmPmToggleStyle(View view, int checkedId) {
        MaterialButton btnAm = view.findViewById(R.id.btn_am); MaterialButton btnPm = view.findViewById(R.id.btn_pm);
        if (checkedId == R.id.btn_am) { btnAm.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.junglegreen)); btnAm.setTextColor(ContextCompat.getColor(requireContext(), R.color.white)); btnPm.setBackgroundColor(Color.TRANSPARENT); btnPm.setTextColor(ContextCompat.getColor(requireContext(), R.color.Fiord));
        } else { btnPm.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.junglegreen)); btnPm.setTextColor(ContextCompat.getColor(requireContext(), R.color.white)); btnAm.setBackgroundColor(Color.TRANSPARENT); btnAm.setTextColor(ContextCompat.getColor(requireContext(), R.color.Fiord)); }
    }
    private void setTypeButtonSelected(MaterialButton btn) { btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.junglegreen)); btn.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.junglegreen)); btn.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.junglegreen)); }
    private void setTypeButtonUnselected(MaterialButton btn) { btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.Fiord)); btn.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.StateGray)); btn.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.Fiord)); }
}