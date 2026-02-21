package com.example.do4e;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do4e.his.history;
import com.example.do4e.ui.Home;

public class MainActivity extends AppCompatActivity {

    Button btnHome, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnHome = (Button) findViewById(R.id.btn_home);
        btnHistory = (Button) findViewById(R.id.btn_history);

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Home.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, history.class);
            startActivity(intent);
        });
    }
}
