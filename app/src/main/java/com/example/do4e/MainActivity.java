package com.example.do4e;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.do4e.navigation.AppNavigator;
import com.example.do4e.navigation.FragmentNavigation;
import com.example.do4e.ui.Schedule.Schedule;
import com.example.do4e.ui.home.Home;
import com.example.do4e.ui.meds.my_medicines;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements FragmentNavigation {

    private AppNavigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1);
            }
        }

        navigator = new AppNavigator(getSupportFragmentManager(), R.id.fragment_container);

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            navigator.replaceFragment(new Home());
        }

        // Wire bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();

            if (id == R.id.home_id) {
                selected = new Home();
            } else if (id == R.id.fav_id) {
                selected = new my_medicines();
            } else if (id == R.id.schedule) {
                selected = new Schedule();
            } else if (id == R.id.plan_id) {
                // Profile fragment — placeholder for now
                selected = new Home();
            }

            if (selected != null) {
                navigator.replaceFragment(selected);
                return true;
            }
            return false;
        });
    }

    @Override
    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        navigator.navigateTo(fragment, addToBackStack);
    }

    @Override
    public void popBackStack() {
        navigator.popBackStack();
    }

    @Override
    public void replaceFragment(Fragment fragment) {
        navigator.replaceFragment(fragment);
    }
}