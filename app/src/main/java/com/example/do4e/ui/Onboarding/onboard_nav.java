package com.example.do4e.ui.Onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do4e.MainActivity;
import com.example.do4e.R;

public class onboard_nav extends AppCompatActivity {

    private OnboardingAdapter adapter;
    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private ImageView[] dots;
    private Button nxtBtn;
    private TextView skipTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboard_nav);

        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.PolshiedWhite));
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

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        nxtBtn = findViewById(R.id.nxt_btn);
        skipTxt = findViewById(R.id.skip_txt);

        adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);

        // Setup Sweet Animation
        viewPager.setPageTransformer(new DepthPageTransformer());

        // Enable user swiping
        viewPager.setUserInputEnabled(true);

        setupIndicators();
        updateIndicators(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);

                if (position == adapter.getItemCount() - 1) {
                    nxtBtn.setText(R.string.btn_getstarted);
                    skipTxt.setVisibility(TextView.INVISIBLE);
                } else {
                    nxtBtn.setText(R.string.btn_next);
                    skipTxt.setVisibility(TextView.VISIBLE);
                }
            }
        });

        nxtBtn.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                navigateToHome();
            }
        });

        skipTxt.setOnClickListener(v -> navigateToHome());
    }

    private void setupIndicators() {
        dots = new ImageView[adapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_inactive));
            dots[i].setLayoutParams(layoutParams);
            dotsLayout.addView(dots[i]);
        }
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < dots.length; i++) {
            if (i == position) {
                dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_active));
            } else {
                dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.indicator_inactive));
            }
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(onboard_nav.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}