package com.example.do4e.ui.meds;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.do4e.R;
import com.example.do4e.db.AppDataBase;

public class meds_no extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meds_no, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Wire "Add Medicine" button → navigate to add_meds using action
        view.findViewById(R.id.btn_add_medicine).setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.action_meds_no_to_add_meds));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if medicines were added while we were away (e.g. user saved one)
        AppDataBase db = AppDataBase.getInstance(requireContext());
        new Thread(() -> {
            int count = db.medDAO().getAllMeds().size();
            if (count > 0 && isAdded()) {
                requireActivity().runOnUiThread(
                        () -> NavHostFragment.findNavController(this).navigate(R.id.action_meds_no_to_my_medicines));
            }
        }).start();
    }
}