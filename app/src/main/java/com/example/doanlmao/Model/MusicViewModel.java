package com.example.doanlmao.Model;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Service.SongScanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicViewModel extends AndroidViewModel {
    private MutableLiveData<List<Song>> songsLiveData = new MutableLiveData<>();
    private MutableLiveData<List<Album>> albumsLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isScanning = new MutableLiveData<>(false);
    private DatabaseHelper dbHelper;
    private SongScanner songScanner;
    private Uri treeUri;
    private boolean isDataLoaded = false; // Thêm biến kiểm tra dữ liệu đã load
    public static final String ACTION_SCAN_FINISHED = "com.example.doanlmao.SCAN_FINISHED";


    public MusicViewModel(Application application) {
        super(application);
        dbHelper = new DatabaseHelper(application);
        songScanner = new SongScanner(application, dbHelper);
        songsLiveData.setValue(dbHelper.getAllSongs()); // Load từ SQLite trước
        if (!songsLiveData.getValue().isEmpty()) {
            isDataLoaded = true;
            updateAlbums(songsLiveData.getValue());
        }
    }

    public LiveData<List<Song>> getSongs() {
        return songsLiveData;
    }

    public LiveData<List<Album>> getAlbums() {
        return albumsLiveData;
    }

    public LiveData<Boolean> isScanning() {
        return isScanning;
    }

    public void setTreeUri(Uri uri) {
        this.treeUri = uri;
        songScanner.saveTreeUri(uri);
    }

    public Uri getTreeUri() {
        return treeUri != null ? treeUri : songScanner.loadTreeUri();
    }

    public void scanSongs() {
        if (treeUri == null) {
            treeUri = songScanner.loadTreeUri();
            if (treeUri == null || !songScanner.hasUriPermission(treeUri)) {
                return;
            }
        }

        if (!isDataLoaded || songsLiveData.getValue().isEmpty()) { // Chỉ quét nếu chưa load hoặc trống
            isScanning.setValue(true);
            songScanner.scanFolderAsync(treeUri, songs -> {
                songsLiveData.postValue(songs);
                updateAlbums(songs);
                isDataLoaded = true;
                isScanning.postValue(false);
            });
        }
    }

    public void refreshSongs() {
        if (treeUri == null) {
            treeUri = songScanner.loadTreeUri();
            if (treeUri == null || !songScanner.hasUriPermission(treeUri)) {
                return;
            }
        }

        isScanning.setValue(true);
        if (MainActivity.musicService != null) {
            MainActivity.musicService.stopSong();
        }

        songScanner.scanFolderAsync(treeUri, songs -> {
            songsLiveData.postValue(songs);
            updateAlbums(songs);
            isDataLoaded = true;
            isScanning.postValue(false);
            if (MainActivity.musicService != null) {
                MainActivity.musicService.setSongList(songs, 0);
                // Gửi broadcast để thông báo quét xong
                Intent intent = new Intent(ACTION_SCAN_FINISHED);
                getApplication().sendBroadcast(intent);
            }
        });
    }




    private void updateAlbums(List<Song> songs) {
        Map<String, Album> albumMap = new HashMap<>();
        List<Album> albums = new ArrayList<>();
        for (Song song : songs) {
            String albumName = song.getAlbum() != null ? song.getAlbum() : "Unknown Album";
            if (!albumMap.containsKey(albumName)) {
                Album album = new Album(albumName, song.getArtist(), song.getArtwork());
                albumMap.put(albumName, album);
                albums.add(album);
            }
            albumMap.get(albumName).addSong(song);
        }
        albumsLiveData.postValue(albums);
    }

    public void addSong(Song song) {
        List<Song> currentSongs = new ArrayList<>(songsLiveData.getValue());
        currentSongs.add(song);
        songsLiveData.setValue(currentSongs);
        updateAlbums(currentSongs);
        dbHelper.addOrUpdateSong(song);
    }

    public void updateSong(Uri audioUri, Song updatedSong) {
        List<Song> currentSongs = new ArrayList<>(songsLiveData.getValue());
        for (int i = 0; i < currentSongs.size(); i++) {
            if (currentSongs.get(i).getPath().equals(audioUri.toString())) {
                currentSongs.set(i, updatedSong);
                break;
            }
        }
        songsLiveData.setValue(currentSongs);
        updateAlbums(currentSongs);
        dbHelper.addOrUpdateSong(updatedSong);
    }

    public void deleteSong(Song song) {
        List<Song> currentSongs = new ArrayList<>(songsLiveData.getValue());
        currentSongs.remove(song);
        songsLiveData.setValue(currentSongs);
        updateAlbums(currentSongs);
        dbHelper.deleteSong(song.getPath());
    }
}

