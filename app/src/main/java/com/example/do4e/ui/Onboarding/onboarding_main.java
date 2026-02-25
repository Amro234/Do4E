package com.example.do4e.ui.Onboarding;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.widget.Button;

import com.example.do4e.R;

public class onboarding_main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding_main);

        getWindow().setNavigationBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.PolshiedWhite));
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController = ViewCompat
                .getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightNavigationBars(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button startBtn = findViewById(R.id.start_btn);
        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(onboarding_main.this, onboard_nav.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
            finish();
        });
    }
}