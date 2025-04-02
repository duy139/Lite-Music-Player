package com.example.doanlmao.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Database.FavoriteDatabase;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private List<Song> favoriteList;
    private FavoriteDatabase favoriteDb;
    private static final String TAG = "FavoriteFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && activity.miniPlayerController != null) {
                    if (dy > 0) {
                        activity.miniPlayerController.setManuallyHidden(true);
                    } else if (dy < 0) {
                        activity.miniPlayerController.setManuallyHidden(false);
                    }
                }
            }
        });

        favoriteDb = new FavoriteDatabase(requireContext());
        favoriteList = new ArrayList<>();

        loadFavorites();

        songAdapter = new SongAdapter(requireContext(), favoriteList);
        recyclerView.setAdapter(songAdapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean-up nếu cần, nhưng không cần close database vì SQLiteOpenHelper tự quản lý
        // favoriteDb.close(); // Không cần gọi close, để SQLiteOpenHelper tự xử lý
    }

    private void loadFavorites() {
        favoriteList.clear();
        favoriteList.addAll(favoriteDb.getAllFavorites());
        if (songAdapter != null) {
            songAdapter.notifyDataSetChanged();
        }
        Log.d(TAG, "Loaded " + favoriteList.size() + " favorite songs");
    }

    public void refreshFavorites() {
        loadFavorites();
    }
}