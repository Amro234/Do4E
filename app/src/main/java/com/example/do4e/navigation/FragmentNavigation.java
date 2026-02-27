package com.example.do4e.navigation;

import androidx.fragment.app.Fragment;

public interface FragmentNavigation {
    void navigateTo(Fragment fragment, boolean addToBackStack);

    void popBackStack();

    void replaceFragment(Fragment fragment);
}
