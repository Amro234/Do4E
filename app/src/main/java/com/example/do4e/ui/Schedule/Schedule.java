package com.example.do4e.ui.Schedule;

import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.do4e.R;
import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedEntity;
import com.example.do4e.navigation.FragmentNavigation;
import com.example.do4e.reminder.ReminderScheduler;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.Locale;

public class Schedule extends Fragment {

    private LinearLayout medContainer;
    private LinearLayout emptyHint;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        medContainer = view.findViewById(R.id.med_container);
        emptyHint = view.findViewById(R.id.empty_hint);

        // Wire time picker on the first static card
        Button btnTimeFirst = view.findViewById(R.id.btn_time_first);
        btnTimeFirst.setOnClickListener(v -> showTimePicker(btnTimeFirst));

        // FAB — add a new dynamic card
        FloatingActionButton fab = view.findViewById(R.id.fab_add_med);
        fab.setOnClickListener(v -> {
            addNewCard();
            emptyHint.setVisibility(View.GONE);
        });

        // Save Schedule button
        view.findViewById(R.id.btn_save_schedule).setOnClickListener(v -> saveSchedule());
    }

    // ─── Add a new dynamic medication card ───────────────────────────────────

    private void addNewCard() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        // Build card container
        CardView card = new CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 32);
        card.setLayoutParams(cardParams);
        card.setRadius(48f);
        card.setCardElevation(6f);
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));

        // Inner layout
        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(48, 48, 48, 48);

        // ── Medicine name label ──
        TextView nameLabel = new TextView(requireContext());
        nameLabel.setText(R.string.Schedule_med_Name);
        nameLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.junglegreen));
        nameLabel.setTextSize(13f);
        nameLabel.setTypeface(null, Typeface.BOLD);
        inner.addView(nameLabel);

        // ── Medicine name EditText ──
        EditText etName = new EditText(requireContext());
        etName.setHint(getString(R.string.Hint_med_Name));
        etName.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_rounded_input));
        etName.setPadding(40, 0, 40, 0);
        etName.setTextColor(ContextCompat.getColor(requireContext(), R.color.Ebony));
        etName.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140);
        etParams.setMargins(0, 16, 0, 0);
        etName.setLayoutParams(etParams);
        inner.addView(etName);

        // ── Time + Days row ──
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 32, 0, 0);
        row.setLayoutParams(rowParams);

        // Time column
        LinearLayout timeCol = new LinearLayout(requireContext());
        timeCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        colParams.setMarginEnd(20);
        timeCol.setLayoutParams(colParams);

        TextView timeLabel = new TextView(requireContext());
        timeLabel.setText(R.string.Schedule_Time);
        timeLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.junglegreen));
        timeLabel.setTextSize(13f);
        timeLabel.setTypeface(null, Typeface.BOLD);
        timeCol.addView(timeLabel);

        Button btnTime = new Button(requireContext());
        btnTime.setText(R.string.ans_time);
        btnTime.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_rounded_input));
        btnTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.junglegreen));
        btnTime.setTextSize(14f);
        btnTime.setAllCaps(false);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140);
        btnParams.setMargins(0, 16, 0, 0);
        btnTime.setLayoutParams(btnParams);
        btnTime.setOnClickListener(v -> showTimePicker(btnTime));
        timeCol.addView(btnTime);
        row.addView(timeCol);

        // Days column
        LinearLayout daysCol = new LinearLayout(requireContext());
        daysCol.setOrientation(LinearLayout.VERTICAL);
        daysCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView daysLabel = new TextView(requireContext());
        daysLabel.setText(R.string.Schedule_Days);
        daysLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.junglegreen));
        daysLabel.setTextSize(13f);
        daysLabel.setTypeface(null, Typeface.BOLD);
        daysCol.addView(daysLabel);

        EditText etDays = new EditText(requireContext());
        etDays.setHint("e.g. 7");
        etDays.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_rounded_input));
        etDays.setPadding(40, 0, 40, 0);
        etDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.Ebony));
        LinearLayout.LayoutParams daysParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140);
        daysParams.setMargins(0, 16, 0, 0);
        etDays.setLayoutParams(daysParams);
        daysCol.addView(etDays);
        row.addView(daysCol);

        inner.addView(row);
        card.addView(inner);
        medContainer.addView(card);
    }

    // ─── Time picker helper ───────────────────────────────────────────────────

    private void showTimePicker(Button targetButton) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        new TimePickerDialog(requireContext(), (view, h, m) -> {
            String amPm = h >= 12 ? "PM" : "AM";
            int hour12 = h % 12;
            if (hour12 == 0)
                hour12 = 12;
            String formatted = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, m, amPm);
            targetButton.setText(formatted);
        }, hour, minute, false).show();
    }

    // ─── Save all cards to DB ─────────────────────────────────────────────────

    private void saveSchedule() {
        // Validate first card
        EditText etFirstName = requireView().findViewById(R.id.et_med_name_first);
        Button btnFirst = requireView().findViewById(R.id.btn_time_first);
        EditText etFirstDays = requireView().findViewById(R.id.et_days_first);

        String firstName = etFirstName.getText().toString().trim();
        String firstTime = btnFirst.getText().toString().trim();
        String firstDays = etFirstDays.getText().toString().trim();

        if (firstName.isEmpty()) {
            etFirstName.setError("Required");
            etFirstName.requestFocus();
            return;
        }

        AppDataBase db = AppDataBase.getInstance(requireContext());

        new Thread(() -> {
            // Save first static card
            int days = firstDays.isEmpty() ? 0 : Integer.parseInt(firstDays);
            int[] hmp = parseTimeStr(firstTime);
            MedEntity first = new MedEntity(firstName, "", firstTime, hmp[0], hmp[1], "Once",
                    days, days == 0, "Pill", "", "");
            db.medDAO().insert(first);

            int notifId = (firstName + firstTime).hashCode();
            ReminderScheduler.schedule(requireContext(), firstName, firstTime, notifId);

            // Save all dynamic cards
            for (int i = 1; i < medContainer.getChildCount(); i++) {
                View cardView = medContainer.getChildAt(i);
                if (!(cardView instanceof CardView))
                    continue;

                CardView card = (CardView) cardView;
                LinearLayout inner = (LinearLayout) card.getChildAt(0);
                LinearLayout row = (LinearLayout) inner.getChildAt(3); // Time+Days row
                LinearLayout timeCol = (LinearLayout) row.getChildAt(0);
                LinearLayout daysCol = (LinearLayout) row.getChildAt(1);

                EditText etName = (EditText) inner.getChildAt(1);
                Button btnT = (Button) timeCol.getChildAt(1);
                EditText etDays = (EditText) daysCol.getChildAt(1);

                String name = etName.getText().toString().trim();
                String time = btnT.getText().toString().trim();
                String dStr = etDays.getText().toString().trim();

                if (name.isEmpty())
                    continue;

                int d = dStr.isEmpty() ? 0 : Integer.parseInt(dStr);
                int[] hm = parseTimeStr(time);
                MedEntity med = new MedEntity(name, "", time, hm[0], hm[1], "Once",
                        d, d == 0, "Pill", "", "");
                db.medDAO().insert(med);

                int nId = (name + time).hashCode();
                ReminderScheduler.schedule(requireContext(), name, time, nId);
            }

            requireActivity().runOnUiThread(() -> showSuccessSheet(firstName, firstTime));
        }).start();
    }

    // ─── Success bottom sheet ─────────────────────────────────────────────────

    private void showSuccessSheet(String medName, String time) {
        BottomSheetDialogFragment sheet = new BottomSheetDialogFragment() {
            @Nullable
            @Override
            public View onCreateView(@NonNull LayoutInflater inflater,
                    @Nullable ViewGroup container,
                    @Nullable Bundle savedInstanceState) {
                return inflater.inflate(R.layout.bottom_sheet_success, container, false);
            }

            @Override
            public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
                // Build "We'll remind you to take Panadol at 08:00 AM."
                // with medName and time highlighted teal
                TextView subtitle = view.findViewById(R.id.tv_success_subtitle);
                String full = "We'll remind you to take " + medName + " at " + time + ".";
                SpannableString span = new SpannableString(full);
                int teal = ContextCompat.getColor(requireContext(), R.color.junglegreen);

                int ns = full.indexOf(medName), ne = ns + medName.length();
                span.setSpan(new ForegroundColorSpan(teal), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new StyleSpan(Typeface.BOLD), ns, ne, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                int ts = full.indexOf(time), te = ts + time.length();
                span.setSpan(new ForegroundColorSpan(teal), ts, te, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                span.setSpan(new StyleSpan(Typeface.BOLD), ts, te, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                subtitle.setText(span);

                // Back to Home
                view.findViewById(R.id.btn_back_to_home).setOnClickListener(v -> {
                    dismiss();
                    if (requireActivity() instanceof FragmentNavigation) {
                        ((FragmentNavigation) requireActivity()).replaceFragment(
                                new com.example.do4e.ui.home.Home());
                    }
                });

                // View Schedule — just dismiss, user is already on Schedule
                view.findViewById(R.id.btn_view_schedule).setOnClickListener(v -> dismiss());
            }
        };
        sheet.show(getParentFragmentManager(), "success_sheet");
    }

    private int[] parseTimeStr(String timeStr) {
        try {
            // Expected format: "08:30 AM"
            String[] parts = timeStr.trim().split(" ");
            String[] hm = parts[0].split(":");
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);
            String amPm = parts[1];

            if (amPm.equalsIgnoreCase("PM") && h < 12)
                h += 12;
            if (amPm.equalsIgnoreCase("AM") && h == 12)
                h = 0;

            return new int[] { h, m };
        } catch (Exception e) {
            return new int[] { 8, 0 }; // fallback
        }
    }
}