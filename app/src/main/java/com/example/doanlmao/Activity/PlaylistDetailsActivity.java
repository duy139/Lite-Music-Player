package com.example.doanlmao.Activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Database.PlaylistDatabase;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private PlaylistDatabase playlistDb;
    private DatabaseHelper songDb;
    private static final String TAG = "PlaylistDetailsActivity";
    private long playlistId;
    private String playlistName;
    private byte[] coverImage;
    private String note;
    private ImageView playlistPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_details);

        recyclerView = findViewById(R.id.recyclerView);
        playlistDb = new PlaylistDatabase(this);
        songDb = new DatabaseHelper(this);

        Intent intent = getIntent();
        playlistId = intent.getLongExtra("playlistId", -1);
        playlistName = intent.getStringExtra("playlistName");
        note = intent.getStringExtra("note");
        coverImage = intent.getByteArrayExtra("coverImage");

        Log.d(TAG, "Playlist ID: " + playlistId + ", Name: " + playlistName + ", Note: " + note);

        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
            playlistPhoto = findViewById(R.id.playlistPhoto);
            if (coverImage != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(coverImage, 0, coverImage.length);
                playlistPhoto.setImageBitmap(bitmap);
            } else {
                playlistPhoto.setImageResource(R.drawable.baseline_music_note_24);
            }
        }

        List<Song> songList = getSongsInPlaylist(playlistId);
        Log.d(TAG, "Songs in playlist: " + (songList != null ? songList.size() : "null"));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(this, songList != null ? songList : new ArrayList<>(), playlistName, coverImage, note, isLandscape);
        songAdapter.setOnBackClickListener(this::finish);
        songAdapter.setOnAddClickListener(this::showAddOrRemoveDialog);
        recyclerView.setAdapter(songAdapter);
    }

    private List<Song> getSongsInPlaylist(long playlistId) {
        List<String> songPaths = playlistDb.getSongsInPlaylist(playlistId);
        List<Song> allSongs = songDb.getAllSongs();
        List<Song> playlistSongs = new ArrayList<>();

        for (String path : songPaths) {
            for (Song song : allSongs) {
                if (song.getPath().equals(path)) {
                    playlistSongs.add(song);
                    break;
                }
            }
        }
        return playlistSongs;
    }

    private void showAddOrRemoveDialog() {
        String[] options = {"Thêm bài hát", "Xóa bài hát"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Tùy chọn")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddSongDialog();
                    } else {
                        showRemoveSongDialog();
                    }
                })
                .show();
    }

    private void showAddSongDialog() {
        List<Song> allSongs = songDb.getAllSongs();
        List<String> songTitles = new ArrayList<>();
        for (Song song : allSongs) {
            songTitles.add(song.getTitle());
        }
        boolean[] checkedItems = new boolean[songTitles.size()];
        List<String> songsInPlaylist = playlistDb.getSongsInPlaylist(playlistId);
        for (int i = 0; i < allSongs.size(); i++) {
            checkedItems[i] = songsInPlaylist.contains(allSongs.get(i).getPath());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm bài hát vào " + playlistName)
                .setMultiChoiceItems(songTitles.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
                    String songPath = allSongs.get(which).getPath();
                    if (isChecked) {
                        playlistDb.addSongToPlaylist(playlistId, songPath);
                    } else {
                        playlistDb.removeSongFromPlaylist(playlistId, songPath);
                    }
                })
                .setPositiveButton("Xong", (dialog, which) -> {
                    refreshSongList();
                    Toast.makeText(this, "Đã cập nhật danh sách bài hát!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRemoveSongDialog() {
        List<Song> songList = getSongsInPlaylist(playlistId);
        if (songList.isEmpty()) {
            Toast.makeText(this, "Không có bài hát để xóa!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> songTitles = new ArrayList<>();
        for (Song song : songList) {
            songTitles.add(song.getTitle());
        }
        boolean[] checkedItems = new boolean[songTitles.size()];

        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa bài hát khỏi " + playlistName)
                .setMultiChoiceItems(songTitles.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
                    // Không cần xử lý ở đây, chỉ lưu trạng thái checked
                })
                .setPositiveButton("Xóa", (dialog, which) -> {
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            playlistDb.removeSongFromPlaylist(playlistId, songList.get(i).getPath());
                        }
                    }
                    refreshSongList();
                    Toast.makeText(this, "Đã xóa bài hát được chọn!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void refreshSongList() {
        List<Song> updatedSongList = getSongsInPlaylist(playlistId);
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        songAdapter = new SongAdapter(this, updatedSongList, playlistName, coverImage, note, isLandscape);
        songAdapter.setOnBackClickListener(this::finish);
        songAdapter.setOnAddClickListener(this::showAddOrRemoveDialog);
        recyclerView.setAdapter(songAdapter);
    }
}