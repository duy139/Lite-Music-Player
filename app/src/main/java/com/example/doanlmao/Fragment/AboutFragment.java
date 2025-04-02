package com.example.doanlmao.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.doanlmao.R;

public class AboutFragment extends Fragment {
    private static final String TAG = "AboutFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        Log.d(TAG, "AboutFragment loaded");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "AboutFragment resumed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "AboutFragment destroyed");
    }
}