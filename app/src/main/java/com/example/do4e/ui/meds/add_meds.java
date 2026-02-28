package com.example.do4e.ui.meds;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.do4e.R;
import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.ReminderScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class add_meds extends Fragment {

    // Form state
    private int selectedMedId = -1; // -1 means new medicine

    private int selectedHour = 8;
    private int selectedMinute = 30;
    private boolean isAm = true;

    private String interval = "Daily";
    private String frequency = "1 time";
    private String medType = "Pill";
    private String instruction = "Before Food";

    private int daysCount = 1;
    private boolean isContinuous = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_meds, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Back button ──────────────────────────────────────────────────────
        view.findViewById(R.id.btn_back)
                .setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        // ── Time selection ───────────────────────────────────────────────────
        TextView tvHour = view.findViewById(R.id.tv_selected_hour);
        TextView tvMinute = view.findViewById(R.id.tv_selected_minute);

        View.OnClickListener timeListener = v -> {
            new TimePickerDialog(requireContext(), (picker, h, m) -> {
                selectedHour = h;
                selectedMinute = m;

                // Update UI: Display 12-hr format
                int h12 = h % 12;
                if (h12 == 0)
                    h12 = 12;
                tvHour.setText(String.format(Locale.getDefault(), "%02d", h12));
                tvMinute.setText(String.format(Locale.getDefault(), "%02d", m));

                // Auto-toggle AM/PM buttons
                MaterialButtonToggleGroup toggleAmPm = view.findViewById(R.id.toggle_am_pm);
                toggleAmPm.check(h >= 12 ? R.id.btn_pm : R.id.btn_am);
                isAm = h < 12;

            }, selectedHour, selectedMinute, false).show();
        };

        tvHour.setOnClickListener(timeListener);
        tvMinute.setOnClickListener(timeListener);
        // Also the containers for better touch target
        ((View) view.findViewById(R.id.tv_selected_hour).getParent()).setOnClickListener(timeListener);
        ((View) view.findViewById(R.id.tv_selected_minute).getParent()).setOnClickListener(timeListener);

        // ── AM / PM toggle ───────────────────────────────────────────────────
        MaterialButtonToggleGroup toggleAmPm = view.findViewById(R.id.toggle_am_pm);
        MaterialButton btnAm = view.findViewById(R.id.btn_am);
        MaterialButton btnPm = view.findViewById(R.id.btn_pm);

        toggleAmPm.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked)
                return;
            isAm = (checkedId == R.id.btn_am);
            updateAmPmToggleStyle(view, checkedId);
        });

        // Initialize style
        updateAmPmToggleStyle(view, R.id.btn_am);

        // ── Interval toggle ──────────────────────────────────────────────────
        MaterialButtonToggleGroup toggleInterval = view.findViewById(R.id.toggle_interval);
        TextView tvFreqLabel = view.findViewById(R.id.tv_freq_label);
        TextView tvUnit = view.findViewById(R.id.tv_duration_unit);
        toggleInterval.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked)
                return;
            if (checkedId == R.id.btn_daily) {
                interval = "Daily";
                tvFreqLabel.setText("Times per Day");
                tvUnit.setText("DAYS");
            } else if (checkedId == R.id.btn_weekly_interval) {
                interval = "Weekly";
                tvFreqLabel.setText("Times per Week");
                tvUnit.setText("WEEKS");
            } else if (checkedId == R.id.btn_monthly_interval) {
                interval = "Monthly";
                tvFreqLabel.setText("Times per Month");
                tvUnit.setText("MONTHS");
            }
        });

        // ── Frequency toggle ─────────────────────────────────────────────────
        MaterialButtonToggleGroup toggleFreq = view.findViewById(R.id.toggle_frequency);
        toggleFreq.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked)
                return;
            if (checkedId == R.id.btn_once)
                frequency = "1 time";
            else if (checkedId == R.id.btn_twice)
                frequency = "2 times";
            else if (checkedId == R.id.btn_thrice)
                frequency = "3 times";
        });

        // ── Medicine type buttons ────────────────────────────────────────────
        MaterialButton btnPill = view.findViewById(R.id.btn_pill);
        MaterialButton btnSyrup = view.findViewById(R.id.btn_syrup);
        MaterialButton btnSyringe = view.findViewById(R.id.btn_syringe);

        View.OnClickListener typeListener = v -> {
            // Reset all to unselected style
            setTypeButtonUnselected(btnPill);
            setTypeButtonUnselected(btnSyrup);
            setTypeButtonUnselected(btnSyringe);
            // Select tapped one
            setTypeButtonSelected((MaterialButton) v);
            if (v.getId() == R.id.btn_pill)
                medType = "Pill";
            else if (v.getId() == R.id.btn_syrup)
                medType = "Syrup";
            else if (v.getId() == R.id.btn_syringe)
                medType = "Syringe";
        };
        btnPill.setOnClickListener(typeListener);
        btnSyrup.setOnClickListener(typeListener);
        btnSyringe.setOnClickListener(typeListener);

        // ── Food instruction buttons ─────────────────────────────────────────
        MaterialButton btnBefore = view.findViewById(R.id.btn_before_food);
        MaterialButton btnDuring = view.findViewById(R.id.btn_during_food);
        MaterialButton btnAfter = view.findViewById(R.id.btn_after_food);

        View.OnClickListener foodListener = v -> {
            setTypeButtonUnselected(btnBefore);
            setTypeButtonUnselected(btnDuring);
            setTypeButtonUnselected(btnAfter);
            setTypeButtonSelected((MaterialButton) v);
            if (v.getId() == R.id.btn_before_food)
                instruction = "Before Food";
            else if (v.getId() == R.id.btn_during_food)
                instruction = "During Food";
            else if (v.getId() == R.id.btn_after_food)
                instruction = "After Food";
        };
        btnBefore.setOnClickListener(foodListener);
        btnDuring.setOnClickListener(foodListener);
        btnAfter.setOnClickListener(foodListener);

        // ── Duration: +/− counter + Continuous switch ───────────────────────
        TextView tvDays = view.findViewById(R.id.tv_days_value);
        View layoutDaysInput = view.findViewById(R.id.layout_days_input);
        View layoutContinuousBadge = view.findViewById(R.id.layout_continuous_badge);
        SwitchMaterial switchContinuous = view.findViewById(R.id.switch_continuous);

        view.findViewById(R.id.btn_days_plus).setOnClickListener(v -> {
            daysCount++;
            tvDays.setText(String.valueOf(daysCount));
        });

        view.findViewById(R.id.btn_days_minus).setOnClickListener(v -> {
            if (daysCount > 1) {
                daysCount--;
                tvDays.setText(String.valueOf(daysCount));
            }
        });

        switchContinuous.setOnCheckedChangeListener((btn, checked) -> {
            isContinuous = checked;
            layoutDaysInput.setVisibility(checked ? View.GONE : View.VISIBLE);
            layoutContinuousBadge.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // ── Reset button ─────────────────────────────────────────────────────
        view.findViewById(R.id.btn_reset).setOnClickListener(v -> resetForm(view));

        // ── Save button ──────────────────────────────────────────────────────
        view.findViewById(R.id.btn_save).setOnClickListener(v -> saveMedicine(view));

        // ── Load if Edit Mode ────────────────────────────────────────────────
        if (getArguments() != null) {
            selectedMedId = getArguments().getInt("med_id", -1);
            if (selectedMedId != -1) {
                loadMedicineData(view);
                ((TextView) view.findViewById(R.id.add_meds_title_text)).setText("Edit Medicine");
            }
        }
    }

    private void loadMedicineData(View view) {
        AppDataBase db = AppDataBase.getInstance(requireContext());
        new Thread(() -> {
            MedEntity med = db.medDAO().getById(selectedMedId);
            if (med == null)
                return;

            requireActivity().runOnUiThread(() -> {
                // Name
                TextInputLayout nameLayout = view.findViewById(R.id.name_meds);
                if (nameLayout.getEditText() != null)
                    nameLayout.getEditText().setText(med.name);

                // Dosage
                TextInputLayout dosageLayout = view.findViewById(R.id.dosage_med);
                if (dosageLayout.getEditText() != null)
                    dosageLayout.getEditText().setText(med.dosage);

                // Time
                selectedHour = med.hour;
                selectedMinute = med.minute;
                updateTimeUI(view);

                // Interval
                MaterialButtonToggleGroup toggleInterval = view.findViewById(R.id.toggle_interval);
                if (med.interval != null) {
                    if (med.interval.equals("Daily"))
                        toggleInterval.check(R.id.btn_daily);
                    else if (med.interval.equals("Weekly"))
                        toggleInterval.check(R.id.btn_weekly_interval);
                    else if (med.interval.equals("Monthly"))
                        toggleInterval.check(R.id.btn_monthly_interval);
                }

                // Frequency
                MaterialButtonToggleGroup toggleFreq = view.findViewById(R.id.toggle_frequency);
                if ("1 time".equals(med.frequency))
                    toggleFreq.check(R.id.btn_once);
                else if ("2 times".equals(med.frequency))
                    toggleFreq.check(R.id.btn_twice);
                else if ("3 times".equals(med.frequency))
                    toggleFreq.check(R.id.btn_thrice);
                frequency = med.frequency;

                // Continuous
                SwitchMaterial switchContinuous = view.findViewById(R.id.switch_continuous);
                switchContinuous.setChecked(med.isContinuous);

                // Days
                if (!med.isContinuous) {
                    daysCount = med.durationDays;
                    ((TextView) view.findViewById(R.id.tv_days_value)).setText(String.valueOf(daysCount));
                }

                // Type
                MaterialButton btnPill = view.findViewById(R.id.btn_pill);
                MaterialButton btnSyrup = view.findViewById(R.id.btn_syrup);
                MaterialButton btnSyringe = view.findViewById(R.id.btn_syringe);

                setTypeButtonUnselected(btnPill);
                setTypeButtonUnselected(btnSyrup);
                setTypeButtonUnselected(btnSyringe);

                if ("Pill".equals(med.medType))
                    setTypeButtonSelected(btnPill);
                else if ("Syrup".equals(med.medType))
                    setTypeButtonSelected(btnSyrup);
                else if ("Syringe".equals(med.medType))
                    setTypeButtonSelected(btnSyringe);
                medType = med.medType;

                // Instructions
                MaterialButton btnBefore = view.findViewById(R.id.btn_before_food);
                MaterialButton btnDuring = view.findViewById(R.id.btn_during_food);
                MaterialButton btnAfter = view.findViewById(R.id.btn_after_food);

                setTypeButtonUnselected(btnBefore);
                setTypeButtonUnselected(btnDuring);
                setTypeButtonUnselected(btnAfter);

                if ("Before Food".equals(med.instruction))
                    setTypeButtonSelected(btnBefore);
                else if ("During Food".equals(med.instruction))
                    setTypeButtonSelected(btnDuring);
                else if ("After Food".equals(med.instruction))
                    setTypeButtonSelected(btnAfter);
                instruction = med.instruction;

                // Notes
                TextInputLayout notesLayout = view.findViewById(R.id.notes_med);
                if (notesLayout.getEditText() != null)
                    notesLayout.getEditText().setText(med.notes);
            });
        }).start();
    }

    private void updateTimeUI(View view) {
        TextView tvHour = view.findViewById(R.id.tv_selected_hour);
        TextView tvMinute = view.findViewById(R.id.tv_selected_minute);
        int h12 = selectedHour % 12;
        if (h12 == 0)
            h12 = 12;
        tvHour.setText(String.format(Locale.getDefault(), "%02d", h12));
        tvMinute.setText(String.format(Locale.getDefault(), "%02d", selectedMinute));

        MaterialButtonToggleGroup toggleAmPm = view.findViewById(R.id.toggle_am_pm);
        toggleAmPm.check(selectedHour >= 12 ? R.id.btn_pm : R.id.btn_am);
        isAm = selectedHour < 12;
    }

    // ─── Save medicine to DB ──────────────────────────────────────────────────

    private void saveMedicine(View view) {
        // Read name
        TextInputLayout nameLayout = view.findViewById(R.id.name_meds);
        TextInputEditText etName = (TextInputEditText) nameLayout.getEditText();
        String name = etName != null ? etName.getText().toString().trim() : "";

        if (name.isEmpty()) {
            nameLayout.setError("Medicine name is required");
            return;
        } else {
            nameLayout.setError(null);
        }

        // Read dosage
        TextInputLayout dosageLayout = view.findViewById(R.id.dosage_med);
        TextInputEditText etDosage = (TextInputEditText) dosageLayout.getEditText();
        String dosage = etDosage != null ? etDosage.getText().toString().trim() : "";

        // Read notes
        TextInputLayout notesLayout = view.findViewById(R.id.notes_med);
        TextInputEditText etNotes = (TextInputEditText) notesLayout.getEditText();
        String notes = etNotes != null ? etNotes.getText().toString().trim() : "";

        // Build time string e.g. "08:30 AM"
        String amPm = isAm ? "AM" : "PM";
        int hour12 = selectedHour % 12;
        if (hour12 == 0)
            hour12 = 12;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d %s",
                hour12, selectedMinute, amPm);

        int finalDays = isContinuous ? 0 : daysCount;

        MedEntity med = new MedEntity(name, dosage, timeStr, selectedHour, selectedMinute,
                interval, frequency, finalDays, isContinuous, medType, instruction, notes);

        if (selectedMedId != -1) {
            med.id_meds = selectedMedId;
        }

        AppDataBase db = AppDataBase.getInstance(requireContext());

        // Capture navController on the main thread before entering background work
        NavController navController = NavHostFragment.findNavController(this);

        new Thread(() -> {
            if (selectedMedId == -1) {
                db.medDAO().insert(med);
            } else {
                db.medDAO().update(med);
            }

            // Schedule daily reminder
            int notifId = (name + timeStr).hashCode();
            ReminderScheduler.schedule(requireContext(), name, timeStr, notifId);

            requireActivity().runOnUiThread(() -> showSuccessSheet(name, timeStr, interval, navController));
        }).start();
    }

    // ─── Success bottom sheet ─────────────────────────────────────────────────

    private void showSuccessSheet(String medName, String time, String interval, NavController navController) {
        SuccessSheet sheet = SuccessSheet.newInstance(medName, time, interval);
        // Store navController for the sheet to use when buttons are tapped
        sheet.setNavController(navController);
        sheet.show(getParentFragmentManager(), "success_sheet");
    }

    /** Must be public static so Android can recreate it from instance state. */
    public static class SuccessSheet extends com.google.android.material.bottomsheet.BottomSheetDialogFragment {

        private NavController navController;

        public static SuccessSheet newInstance(String medName, String time, String interval) {
            SuccessSheet sheet = new SuccessSheet();
            Bundle args = new Bundle();
            args.putString("medName", medName);
            args.putString("time", time);
            args.putString("interval", interval);
            sheet.setArguments(args);
            return sheet;
        }

        public void setNavController(NavController nc) {
            this.navController = nc;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.bottom_sheet_success, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            String medName = requireArguments().getString("medName", "");
            String time = requireArguments().getString("time", "");

            TextView subtitle = view.findViewById(R.id.tv_success_subtitle);
            String full = "We'll remind you to take " + medName + " at " + time + ".";
            SpannableString span = new SpannableString(full);
            int teal = ContextCompat.getColor(requireContext(), R.color.junglegreen);

            int ns = full.indexOf(medName), ne = ns + medName.length();
            span.setSpan(new ForegroundColorSpan(teal), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StyleSpan(Typeface.BOLD), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int ts = full.lastIndexOf(time);
            int te = ts + time.length();
            if (ts >= 0) {
                span.setSpan(new ForegroundColorSpan(teal), ts, te, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new StyleSpan(Typeface.BOLD), ts, te, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            subtitle.setText(span);

            view.findViewById(R.id.btn_back_to_home).setOnClickListener(v -> {
                dismiss();
                if (navController != null) {
                    Bundle returnArgs = new Bundle();
                    returnArgs.putString("start_interval", requireArguments().getString("interval", "Daily"));
                    navController.navigate(R.id.fav_id, returnArgs);
                }
            });

            view.findViewById(R.id.btn_view_schedule).setOnClickListener(v -> {
                dismiss();
                if (navController != null)
                    navController.navigate(R.id.schedule);
            });
        }
    }

    // ─── Reset form ───────────────────────────────────────────────────────────

    private void resetForm(View view) {
        TextInputLayout nameLayout = view.findViewById(R.id.name_meds);
        if (nameLayout.getEditText() != null)
            nameLayout.getEditText().setText("");

        TextInputLayout dosageLayout = view.findViewById(R.id.dosage_med);
        if (dosageLayout.getEditText() != null)
            dosageLayout.getEditText().setText("");

        TextInputLayout notesLayout = view.findViewById(R.id.notes_med);
        if (notesLayout.getEditText() != null)
            notesLayout.getEditText().setText("");

        // Reset toggles
        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_am_pm))
                .check(R.id.btn_am);
        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_interval))
                .check(R.id.btn_daily);
        ((MaterialButtonToggleGroup) view.findViewById(R.id.toggle_frequency))
                .check(R.id.btn_once);

        // Reset state vars
        selectedMedId = -1;
        isAm = true;
        interval = "Daily";
        frequency = "1 time";
        medType = "Pill";
        instruction = "Before Food";
        daysCount = 1;
        isContinuous = false;

        TextView tvDays = view.findViewById(R.id.tv_days_value);
        tvDays.setText("1");

        SwitchMaterial sw = view.findViewById(R.id.switch_continuous);
        sw.setChecked(false);

        view.findViewById(R.id.layout_days_input).setVisibility(View.VISIBLE);
        view.findViewById(R.id.layout_continuous_badge).setVisibility(View.GONE);
    }

    // ─── Button style helpers ─────────────────────────────────────────────────

    private void updateAmPmToggleStyle(View view, int checkedId) {
        MaterialButton btnAm = view.findViewById(R.id.btn_am);
        MaterialButton btnPm = view.findViewById(R.id.btn_pm);

        if (checkedId == R.id.btn_am) {
            btnAm.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.junglegreen));
            btnAm.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnPm.setBackgroundColor(Color.TRANSPARENT);
            btnPm.setTextColor(ContextCompat.getColor(requireContext(), R.color.Fiord));
        } else {
            btnPm.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.junglegreen));
            btnPm.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            btnAm.setBackgroundColor(Color.TRANSPARENT);
            btnAm.setTextColor(ContextCompat.getColor(requireContext(), R.color.Fiord));
        }
    }

    private void setTypeButtonSelected(MaterialButton btn) {
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.junglegreen));
        btn.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.junglegreen));
        btn.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.junglegreen));
    }

    private void setTypeButtonUnselected(MaterialButton btn) {
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.Fiord));
        btn.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.StateGray));
        btn.setIconTint(ContextCompat.getColorStateList(requireContext(), R.color.Fiord));
    }
}