package com.example.doanlmao.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;

import java.util.ArrayList;
import java.util.List;

    public class AlbumDetailsActivity extends AppCompatActivity {
        private RecyclerView recyclerView;
        private SongAdapter songAdapter;
        private DatabaseHelper dbHelper;
        private static final String TAG = "AlbumDetails";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_album_details);

            recyclerView = findViewById(R.id.recyclerView);
            dbHelper = new DatabaseHelper(this);

            Intent intent = getIntent();
            String albumName = intent.getStringExtra("albumName");
            String artistName = intent.getStringExtra("artistName");
            byte[] artwork = intent.getByteArrayExtra("artwork");

            Log.d(TAG, "Album: " + albumName + ", Artist: " + artistName);

            List<Song> songList = getSongsByAlbum(albumName);
            Log.d(TAG, "Songs in album: " + (songList != null ? songList.size() : "null"));

            if (songList != null && !songList.isEmpty()) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                songAdapter = new SongAdapter(this, songList, albumName, artistName, artwork);
                songAdapter.setOnBackClickListener(this::finish); // Xử lý nút back
                recyclerView.setAdapter(songAdapter);
            } else {
                Log.e(TAG, "No songs found for album: " + albumName);
                Toast.makeText(this, "Không có bài hát trong album này", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        private List<Song> getSongsByAlbum(String albumName) {
            List<Song> allSongs = dbHelper.getAllSongs();
            List<Song> albumSongs = new ArrayList<>();

            for (Song song : allSongs) {
                String songAlbum = song.getAlbum() != null && !song.getAlbum().trim().isEmpty() ? song.getAlbum() : "Unknown Album";
                if (songAlbum.equals(albumName)) {
                    albumSongs.add(song);
                }
            }
            return albumSongs;
        }
    }