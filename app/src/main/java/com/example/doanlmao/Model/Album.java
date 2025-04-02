package com.example.doanlmao.Model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Album implements Parcelable {
    private String name;
    private String artist;
    private byte[] artwork;
    private List<Song> songs;

    // Constructor
    public Album(String name, String artist, byte[] artwork) {
        this.name = name;
        this.artist = artist;
        this.artwork = artwork;
        this.songs = new ArrayList<>();
    }

    // Getter
    public String getName() { return name; }
    public String getArtist() { return artist; }
    public byte[] getArtwork() { return artwork; }
    public List<Song> getSongs() { return songs; }

    public void addSong(Song song) {
        songs.add(song);
    }

    // Parcelable implementation
    protected Album(Parcel in) {
        name = in.readString();
        artist = in.readString();
        artwork = in.createByteArray();
        songs = in.createTypedArrayList(Song.CREATOR);
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(artist);
        dest.writeByteArray(artwork);
        dest.writeTypedList(songs);
    }
}