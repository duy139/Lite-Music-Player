package com.example.doanlmao.Misc;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Model.Song;

import java.util.ArrayList;
import java.util.List;

public class SongListManager {
    private List<Song> songList;
    private List<Song> filteredSongList;
    private final SongAdapter songAdapter;
    private final DatabaseHelper dbHelper;
    private final SongSorter songSorter;
    private static final String TAG = "SongListManager";

    public SongListManager(Context context, RecyclerView recyclerView) {
        this.songList = new ArrayList<>();
        this.filteredSongList = new ArrayList<>();
        this.dbHelper = new DatabaseHelper(context);
        this.songSorter = new SongSorter(context);
        this.songAdapter = new SongAdapter(context, filteredSongList);
        recyclerView.setAdapter(songAdapter);
        SongAdapter.globalSongList = songList;
    }

    public void loadSongsFromDatabase() {
        songList = dbHelper.getAllSongs();
        for (Song song : songList) {
            if (!song.getPath().startsWith("content://")) {
                Log.w(TAG, "Invalid URI detected: " + song.getPath() + ", forcing rescan...");
                songList.clear();
                dbHelper.clearSongs();
                return;
            }
        }
        resetFilteredSongList();
        applyLastSort();
    }

    public void setSongList(List<Song> songs) {
        songList.clear();
        songList.addAll(songs);
        resetFilteredSongList();
        applyLastSort();
        SongAdapter.globalSongList = songList;
        songAdapter.notifyDataSetChanged();
        Log.d(TAG, "Updated UI with " + songList.size() + " songs, MusicService unchanged");
    }

    public void addSong(Song newSong) {
        dbHelper.addOrUpdateSong(newSong);
        Log.d(TAG, "Added new song to SQLite: " + newSong.getTitle());

        songList.add(newSong);
        filteredSongList.add(newSong);
        SongAdapter.globalSongList = songList;

        if (MainActivity.musicService != null) {
            MainActivity.musicService.setSongList(songList, MainActivity.musicService.getCurrentSongIndex());
            Log.d(TAG, "Updated MusicService with new song list");
        }

        songAdapter.notifyItemInserted(filteredSongList.size() - 1);
    }

    public void updateSong(Uri audioUri, Song updatedSong) {
        String path = audioUri.toString();
        int songIndex = -1;
        int filteredIndex = -1;

        for (int i = 0; i < songList.size(); i++) {
            if (songList.get(i).getPath().equals(path)) {
                songIndex = i;
                break;
            }
        }

        for (int i = 0; i < filteredSongList.size(); i++) {
            if (filteredSongList.get(i).getPath().equals(path)) {
                filteredIndex = i;
                break;
            }
        }

        if (songIndex != -1 && filteredIndex != -1) {
            songList.set(songIndex, updatedSong);
            filteredSongList.set(filteredIndex, updatedSong);
            SongAdapter.globalSongList = songList;

            dbHelper.addOrUpdateSong(updatedSong);
            Log.d(TAG, "Updated song in SQLite: " + updatedSong.getTitle());

            if (MainActivity.musicService != null) {
                MainActivity.musicService.setSongList(songList, MainActivity.musicService.getCurrentSongIndex());
                Log.d(TAG, "Updated MusicService with new song list");
            }

            songAdapter.notifyItemChanged(filteredIndex);
        } else {
            Log.w(TAG, "Song not found in list");
        }
    }

    public void deleteSong(Song song, int position) {
        dbHelper.deleteSong(song.getPath());
        songList.remove(song);
        filteredSongList.remove(position);
        songAdapter.notifyItemRemoved(position);
        SongAdapter.globalSongList = songList;
    }

    public void refreshSongs(List<Song> songs, List<String> oldSongPaths) {
        dbHelper.clearSongs();
        List<Song> sortedSongs = new ArrayList<>();
        for (String oldPath : oldSongPaths) {
            for (Song newSong : songs) {
                if (newSong.getPath().equals(oldPath)) {
                    sortedSongs.add(newSong);
                    break;
                }
            }
        }
        for (Song newSong : songs) {
            if (!oldSongPaths.contains(newSong.getPath())) {
                sortedSongs.add(newSong);
            }
        }

        songList.clear();
        songList.addAll(sortedSongs);
        resetFilteredSongList();
        applyLastSort();
        SongAdapter.globalSongList = songList;
        if (MainActivity.musicService != null) {
            MainActivity.musicService.setSongList(songList, MainActivity.musicService.getCurrentSongIndex());
            Log.d(TAG, "Updated MusicService with " + songList.size() + " songs");
        }
        songAdapter.notifyDataSetChanged();
    }

    private void resetFilteredSongList() {
        filteredSongList.clear();
        filteredSongList.addAll(songList);
    }

    public void applyLastSort() {
        int sortType = songSorter.getLastSortType();
        boolean ascending = songSorter.getLastSortAscending();
        songSorter.sortSongs(filteredSongList, sortType, ascending);
    }

    public void sortSongs(int sortType, boolean ascending) {
        songSorter.sortSongs(filteredSongList, sortType, ascending);
        songAdapter.notifyDataSetChanged();
    }

    public List<Song> getSongList() {
        return new ArrayList<>(songList);
    }

    public List<Song> getFilteredSongList() {
        return filteredSongList;
    }

    public SongAdapter getSongAdapter() {
        return songAdapter;
    }
}