package com.example.do4e;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;
import android.app.TimePickerDialog;
import java.util.Calendar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnTimePicker = (Button) findViewById(R.id.btn_time);
        etMedName = (EditText) findViewById(R.id.et_med_name);
        tvIndex = (TextView) findViewById(R.id.tv_index);
        txtTime = (TextView) findViewById(R.id.txtTime);
        btnadd = (Button) findViewById(R.id.extended_fab);
        medContainer = findViewById(R.id.med_container);
        btnSave = (Button) findViewById(R.id.btn_save);
        btnadd = (Button) findViewById(R.id.extended_fab);

        btnadd.setOnClickListener(v -> addNewRow());
        btnSave.setOnClickListener(v -> saveData());

        btnTimePicker.setOnClickListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onClick(View v) {
        if (v == btnTimePicker) {
            // Get Current Time
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
        }
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

        for (int i = 0; i < medContainer.getChildCount(); i++) {

            LinearLayout row = (LinearLayout) medContainer.getChildAt(i);

            EditText medName = (EditText) row.getChildAt(1);
            TextView timeTxt = (TextView) row.getChildAt(3);

            String name = medName.getText().toString();
            String time = timeTxt.getText().toString();

            android.util.Log.d(TAG, "Medicine: " + name + " | Time: " + time);
        }
    }
}
