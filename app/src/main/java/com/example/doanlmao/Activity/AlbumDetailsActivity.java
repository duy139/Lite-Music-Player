package com.example.doanlmao.Activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
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
    private ImageView albumPhoto;

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

        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
            albumPhoto = findViewById(R.id.albumPhoto);

            if (artwork != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork.length);
                albumPhoto.setImageBitmap(bitmap);
            } else {
                albumPhoto.setImageResource(R.drawable.baseline_music_note_24);
            }
        }

        List<Song> songList = getSongsByAlbum(albumName);
        Log.d(TAG, "Songs in album: " + (songList != null ? songList.size() : "null"));

        if (songList != null && !songList.isEmpty()) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            songAdapter = new SongAdapter(this, songList, albumName, artistName, artwork, isLandscape);
            songAdapter.setShowHeader(true);
            songAdapter.setOnBackClickListener(this::finish); // Nút Back trong header xử lý ở cả dọc và ngang
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
