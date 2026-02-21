package com.example.do4e.ui.Home;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.do4e.R;
import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedEntity;
import com.example.do4e.ui.his.history;
import com.example.do4e.reminder.ReminderScheduler;

import java.util.Calendar;

public class Home extends AppCompatActivity {

    private static final String TAG = "Home";

    LinearLayout medContainer;
    Button btnSave;
    Button btnadd;
    private int mHour, mMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnadd = findViewById(R.id.extended_fab);
        medContainer = findViewById(R.id.med_container);
        btnSave = findViewById(R.id.btn_save);

        btnadd.setOnClickListener(v -> addNewRow());
        btnSave.setOnClickListener(v -> saveData());

        // Add the first dynamic row initially
        addNewRow();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void addNewRow() {
        LinearLayout newRow = new LinearLayout(this);
        newRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView index = new TextView(this);
        index.setText(String.valueOf(medContainer.getChildCount() + 1));
        index.setPadding(20, 0, 20, 0);

        EditText medName = new EditText(this);
        medName.setHint("Add Meds");

        Button timeBtn = new Button(this);
        timeBtn.setText("TIME");

        TextView timeTxt = new TextView(this);
        timeTxt.setText("00:00");
        timeTxt.setPadding(20, 0, 20, 0);

        timeBtn.setOnClickListener(v -> {
            // Warn if medicine name is not filled yet
            if (medName.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter a medicine name first", Toast.LENGTH_SHORT).show();
            }
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(this,
                    (view, h, m) -> {
                        String amPm = h >= 12 ? "PM" : "AM";
                        int displayHour = h % 12;
                        if (displayHour == 0)
                            displayHour = 12;
                        String time = String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, m,
                                amPm);
                        timeTxt.setText(time);
                        // Remind again after picking if name is still empty
                        if (medName.getText().toString().trim().isEmpty()) {
                            Toast.makeText(Home.this, "Don't forget to enter a medicine name!", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }, hour, minute, false);
            dialog.show();
        });

        Button deleteBtn = new Button(this);
        deleteBtn.setText("DELETE");
        deleteBtn.setOnClickListener(v -> {
            medContainer.removeView(newRow);
            refreshIndices();
        });

        newRow.addView(index);
        newRow.addView(medName);
        newRow.addView(timeBtn);
        newRow.addView(timeTxt);
        newRow.addView(deleteBtn);

        medContainer.addView(newRow);
    }

    private void refreshIndices() {
        for (int i = 0; i < medContainer.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) medContainer.getChildAt(i);
            TextView index = (TextView) row.getChildAt(0);
            index.setText(String.valueOf(i + 1));
        }
    }

    private void saveData() {
        // Prevent saving when all rows have been deleted
        if (medContainer.getChildCount() == 0) {
            Toast.makeText(this, "Please add at least one medicine before saving", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate all rows have a medicine name before saving
        for (int i = 0; i < medContainer.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) medContainer.getChildAt(i);
            EditText medName = (EditText) row.getChildAt(1);
            if (medName.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Row " + (i + 1) + ": Please enter a medicine name", Toast.LENGTH_SHORT).show();
                return; // stop and don't save anything
            }
        }

        AppDataBase db = AppDataBase.getInstance(this);

        // Run DB operations on a background thread
        new Thread(() -> {
            boolean anyDuplicate = false;

            for (int i = 0; i < medContainer.getChildCount(); i++) {
                LinearLayout row = (LinearLayout) medContainer.getChildAt(i);

                EditText medName = (EditText) row.getChildAt(1);
                TextView timeTxt = (TextView) row.getChildAt(3);

                String name = medName.getText().toString().trim();
                String time = timeTxt.getText().toString().trim();

                if (!name.isEmpty()) {
                    // Check for duplicate
                    MedEntity existing = db.medDAO().getByName(name);
                    if (existing != null) {
                        anyDuplicate = true;
                        runOnUiThread(() -> Toast.makeText(Home.this,
                                "\"" + name + "\" is already saved. Update it if you want.",
                                Toast.LENGTH_LONG).show());
                    } else {
                        MedEntity entity = new MedEntity(name, time);
                        db.medDAO().insert(entity);

                        // scheduling the reminder
                        int notifId = (name + time).hashCode();
                        ReminderScheduler.schedule(Home.this, name, time, notifId);
                    }
                }
            }

            // Navigate to History on the main thread after saving
            runOnUiThread(() -> {
                Toast.makeText(Home.this, "Saved!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Home.this, history.class);
                startActivity(intent);
            });
        }).start();
    }
}
