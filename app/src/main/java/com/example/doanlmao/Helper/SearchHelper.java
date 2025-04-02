package com.example.doanlmao.Helper;

import com.example.doanlmao.Model.Song;
import com.example.doanlmao.Model.Album;

import java.util.ArrayList;
import java.util.List;

public class SearchHelper {
    public static List<Song> searchSongs(List<Song> songList, String query) {
        List<Song> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(songList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Song song : songList) {
                if (song.getTitle().toLowerCase().contains(lowerQuery) ||
                        song.getArtist().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(song);
                }
            }
        }
        return filteredList;
    }

    public static List<Album> searchAlbums(List<Album> albumList, String query) {
        List<Album> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(albumList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Album album : albumList) {
                if (album.getName().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(album);
                }
            }
        }
        return filteredList;
    }
}