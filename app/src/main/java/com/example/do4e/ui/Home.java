package com.example.do4e.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.example.do4e.his.history;

import java.util.Calendar;

public class Home extends AppCompatActivity {

    private static final String TAG = "Home";

    LinearLayout medContainer;
    Button btnSave;
    Button btnadd;
    Button btnTimePicker;
    EditText etMedName;
    TextView tvIndex, txtTime;
    private int mHour, mMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnTimePicker = findViewById(R.id.btn_time);
        etMedName = findViewById(R.id.et_med_name);
        tvIndex = findViewById(R.id.tv_index);
        txtTime = findViewById(R.id.txtTime);
        btnadd = findViewById(R.id.extended_fab);
        medContainer = findViewById(R.id.med_container);
        btnSave = findViewById(R.id.btn_save);

        btnadd.setOnClickListener(v -> addNewRow());
        btnSave.setOnClickListener(v -> saveData());

        btnTimePicker.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            mHour = c.get(Calendar.HOUR_OF_DAY);
            mMinute = c.get(Calendar.MINUTE);

            // Launch Time Picker Dialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        String time = String.format("%02d:%02d", hourOfDay, minute);
                        txtTime.setText(time);
                    }, mHour, mMinute, false);
            timePickerDialog.show();
        });

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
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(this,
                    (view, h, m) -> {
                        String time = String.format("%02d:%02d", h, m);
                        timeTxt.setText(time);
                    }, hour, minute, true);
            dialog.show();
        });

        newRow.addView(index);
        newRow.addView(medName);
        newRow.addView(timeBtn);
        newRow.addView(timeTxt);

        medContainer.addView(newRow);
    }

    private void saveData() {
        AppDataBase db = AppDataBase.getInstance(this);

        // Run DB operations on a background thread
        new Thread(() -> {
            for (int i = 0; i < medContainer.getChildCount(); i++) {
                LinearLayout row = (LinearLayout) medContainer.getChildAt(i);

                EditText medName = (EditText) row.getChildAt(1);
                TextView timeTxt = (TextView) row.getChildAt(3);

                String name = medName.getText().toString().trim();
                String time = timeTxt.getText().toString().trim();

                if (!name.isEmpty()) {
                    MedEntity entity = new MedEntity(name, time);
                    db.medDAO().insert(entity);
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
