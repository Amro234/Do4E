package com.example.do4e.ui.Onboarding;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingAdapter extends FragmentStateAdapter {

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new on_board1();
            case 1:
                return new on_board2();
            case 2:
                return new on_board3();
            default:
                return new on_board1();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
