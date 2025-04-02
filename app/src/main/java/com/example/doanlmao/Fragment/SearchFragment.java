package com.example.doanlmao.Fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.doanlmao.Activity.AlbumDetailsActivity;
import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Adapter.AlbumAdapter;
import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Helper.SearchHelper;
import com.example.doanlmao.Model.Album;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFragment extends Fragment {
    private EditText searchEditText;
    private RecyclerView searchRecyclerView;
    private Button songsButton, albumsButton;
    private SongAdapter songAdapter;
    private AlbumAdapter albumAdapter;
    private List<Song> songList;
    private List<Album> albumList;
    private List<Song> filteredSongList;
    private List<Album> filteredAlbumList;
    private DatabaseHelper dbHelper;
    private static final String TAG = "SearchFragment";
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isShowingSongs = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchEditText = view.findViewById(R.id.searchEditText);
        searchRecyclerView = view.findViewById(R.id.searchRecyclerView);
        songsButton = view.findViewById(R.id.songsButton);
        albumsButton = view.findViewById(R.id.albumsButton);

        // Khởi tạo với LinearLayoutManager cho bài hát
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        dbHelper = new DatabaseHelper(getContext());
        songList = new ArrayList<>();
        albumList = new ArrayList<>();
        filteredSongList = new ArrayList<>();
        filteredAlbumList = new ArrayList<>();

        songAdapter = new SongAdapter(getContext(), filteredSongList);
        albumAdapter = new AlbumAdapter(getContext(), filteredAlbumList, (album, position) -> {
            Intent intent = new Intent(getContext(), AlbumDetailsActivity.class);
            intent.putExtra("albumName", album.getName());
            intent.putExtra("artistName", album.getArtist());
            intent.putExtra("artwork", album.getArtwork());
            startActivity(intent);
        });

        searchRecyclerView.setAdapter(songAdapter);
        updateButtonStates();

        songsButton.setOnClickListener(v -> {
            if (!isShowingSongs) {
                isShowingSongs = true;
                searchRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
                searchRecyclerView.setAdapter(songAdapter);
                filterData(searchEditText.getText().toString());
                updateButtonStates();
            }
        });

        albumsButton.setOnClickListener(v -> {
            if (isShowingSongs) {
                isShowingSongs = false;
                // Kiểm tra orientation và set số cột cho album
                boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                searchRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), isLandscape ? 4 : 2));
                searchRecyclerView.setAdapter(albumAdapter);
                filterData(searchEditText.getText().toString());
                updateButtonStates();
            }
        });

        loadAllDataAsync();

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Log.d(TAG, "onCreateView finished");
        return view;
    }

    private void loadAllDataAsync() {
        Log.d(TAG, "Loading data in background");
        new Thread(() -> {
            List<Song> allSongs = dbHelper.getAllSongs();
            Map<String, Album> albumMap = new HashMap<>();

            songList.clear();
            albumList.clear();
            filteredSongList.clear();
            filteredAlbumList.clear();

            songList.addAll(allSongs);

            for (Song song : allSongs) {
                String albumName = song.getAlbum() != null ? song.getAlbum() : "Unknown Album";
                if (!albumMap.containsKey(albumName)) {
                    Album album = new Album(albumName, song.getArtist(), song.getArtwork());
                    albumMap.put(albumName, album);
                    albumList.add(album);
                }
                albumMap.get(albumName).addSong(song);
            }

            filteredSongList.addAll(songList);
            filteredAlbumList.addAll(albumList);

            mainHandler.post(() -> {
                if (isShowingSongs) {
                    songAdapter.notifyDataSetChanged();
                } else {
                    albumAdapter.notifyDataSetChanged();
                }
                Log.d(TAG, "Loaded " + songList.size() + " songs and " + albumList.size() + " albums");
            });
        }).start();
    }

    private void filterData(String query) {
        if (isShowingSongs) {
            filteredSongList.clear();
            filteredSongList.addAll(SearchHelper.searchSongs(songList, query));
            songAdapter.notifyDataSetChanged();
            Log.d(TAG, "Filtered - Songs: " + filteredSongList.size());
        } else {
            filteredAlbumList.clear();
            filteredAlbumList.addAll(SearchHelper.searchAlbums(albumList, query));
            albumAdapter.notifyDataSetChanged();
            Log.d(TAG, "Filtered - Albums: " + filteredAlbumList.size());
        }
    }

    private void updateButtonStates() {
        songsButton.setEnabled(!isShowingSongs);
        albumsButton.setEnabled(isShowingSongs);

        // Lấy màu từ theme Material You
        TypedValue primaryValue = new TypedValue();
        TypedValue surfaceValue = new TypedValue();
        TypedValue onPrimaryValue = new TypedValue();
        TypedValue onSurfaceValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, primaryValue, true);
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, surfaceValue, true);
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, onPrimaryValue, true);
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, onSurfaceValue, true);
        int colorPrimary = primaryValue.data;       // Màu đậm
        int colorSurface = surfaceValue.data;       // Màu nền
        int colorOnPrimary = onPrimaryValue.data;   // Trắng cho nền đậm
        int colorOnSurface = onSurfaceValue.data;   // Đen hoặc xám cho nền nhạt

        // Cả sáng và tối: Nút chọn đậm (colorPrimary), không chọn nhạt (colorSurface)
        if (isShowingSongs) {
            songsButton.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            songsButton.setTextColor(colorOnPrimary); // Trắng
            albumsButton.setBackgroundTintList(ColorStateList.valueOf(colorSurface));
            albumsButton.setTextColor(colorOnSurface); // Đen hoặc xám
        } else {
            songsButton.setBackgroundTintList(ColorStateList.valueOf(colorSurface));
            songsButton.setTextColor(colorOnSurface);
            albumsButton.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            albumsButton.setTextColor(colorOnPrimary);
        }
    }
}