package com.example.doanlmao.Misc;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.doanlmao.Model.Album;
import com.example.doanlmao.Model.Playlist;
import com.example.doanlmao.Model.Song;

import java.util.Collections;
import java.util.List;

public class SongSorter {
    private static final String PREFS_NAME = "SongSorterPrefs";
    private static final String PREF_SORT_TYPE = "sort_type";
    private static final String PREF_SORT_ASCENDING = "sort_ascending";

    public static final int SORT_BY_TITLE = 0;
    public static final int SORT_BY_ARTIST = 1;
    public static final int SORT_BY_YEAR = 2;
    public static final int SORT_BY_DURATION = 3;

    private final SharedPreferences prefs;

    public SongSorter(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Lấy kiểu sắp xếp cuối cùng mà người dùng chọn
    public int getLastSortType() {
        return prefs.getInt(PREF_SORT_TYPE, SORT_BY_TITLE); // Mặc định là A-Z
    }

    // Lấy trạng thái ascending/descending cuối cùng
    public boolean getLastSortAscending() {
        return prefs.getBoolean(PREF_SORT_ASCENDING, true); // Mặc định là ascending (A-Z)
    }

    // Lưu kiểu sắp xếp và trạng thái ascending/descending
    public void saveSortPreference(int sortType, boolean ascending) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_SORT_TYPE, sortType);
        editor.putBoolean(PREF_SORT_ASCENDING, ascending);
        editor.apply();
    }

    // Hàm sắp xếp danh sách bài hát
    public void sortSongs(List<Song> songList, int sortType, boolean ascending) {
        switch (sortType) {
            case SORT_BY_TITLE:
                Collections.sort(songList, (song1, song2) -> {
                    int result = song1.getTitle().compareToIgnoreCase(song2.getTitle());
                    return ascending ? result : -result;
                });
                break;

            case SORT_BY_ARTIST:
                Collections.sort(songList, (song1, song2) -> {
                    String artist1 = song1.getArtist() != null ? song1.getArtist() : "";
                    String artist2 = song2.getArtist() != null ? song2.getArtist() : "";
                    int result = artist1.compareToIgnoreCase(artist2);
                    return ascending ? result : -result;
                });
                break;

            case SORT_BY_YEAR:
                Collections.sort(songList, (song1, song2) -> {
                    int year1 = song1.getYear();
                    int year2 = song2.getYear();
                    int result = Integer.compare(year1, year2);
                    return ascending ? result : -result;
                });
                break;

            case SORT_BY_DURATION:
                Collections.sort(songList, (song1, song2) -> {
                    // Giả sử duration dạng "mm:ss", chuyển thành giây để so sánh
                    int duration1 = parseDurationToSeconds(song1.getDuration());
                    int duration2 = parseDurationToSeconds(song2.getDuration());
                    int result = Integer.compare(duration1, duration2);
                    return ascending ? result : -result;
                });
                break;
        }
    }

    // Hàm hỗ trợ chuyển duration từ "mm:ss" sang giây
    private int parseDurationToSeconds(String duration) {
        try {
            String[] parts = duration.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        } catch (Exception e) {
            return 0; // Nếu có lỗi, trả về 0
        }
    }


    // Hàm sắp xếp danh sách album
    public void sortAlbums(List<Album> albumList, int sortType, boolean ascending) {
        switch (sortType) {
            case SORT_BY_TITLE:
                Collections.sort(albumList, (album1, album2) -> {
                    int result = album1.getName().compareToIgnoreCase(album2.getName());
                    return ascending ? result : -result;
                });
                break;

            case SORT_BY_ARTIST:
                Collections.sort(albumList, (album1, album2) -> {
                    String artist1 = album1.getArtist() != null ? album1.getArtist() : "";
                    String artist2 = album2.getArtist() != null ? album2.getArtist() : "";
                    int result = artist1.compareToIgnoreCase(artist2);
                    return ascending ? result : -result;
                });
                break;
        }
    }


    public void sortPlaylists(List<Playlist> playlistList, int sortType, boolean ascending) {
        switch (sortType) {
            case SORT_BY_TITLE:
                Collections.sort(playlistList, (playlist1, playlist2) -> {
                    int result = playlist1.getName().compareToIgnoreCase(playlist2.getName());
                    return ascending ? result : -result;
                });
                break;
        }
    }
}