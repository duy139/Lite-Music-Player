package com.example.doanlmao.Model;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    private String title;
    private String artist;
    private String duration;
    private String path;
    private byte[] artwork;
    private String album;
    private int year; // Thêm field year

    public Song(String title, String artist, String duration, String path, byte[] artwork, String album, int year) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.path = path;
        this.artwork = artwork;
        this.album = album;
        this.year = year;
    }

    protected Song(Parcel in) {
        title = in.readString();
        artist = in.readString();
        duration = in.readString();
        path = in.readString();
        artwork = in.createByteArray();
        album = in.readString();
        year = in.readInt(); // Đọc year từ Parcel
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(duration);
        dest.writeString(path);
        dest.writeByteArray(artwork);
        dest.writeString(album);
        dest.writeInt(year); // Ghi year vào Parcel
    }

    // Getters
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getDuration() { return duration; }
    public String getPath() { return path; }
    public byte[] getArtwork() { return artwork; }
    public String getAlbum() { return album; }
    public int getYear() { return year; } // Getter cho year

    // Setters (nếu cần)
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setPath(String path) { this.path = path; }
    public void setArtwork(byte[] artwork) { this.artwork = artwork; }
    public void setAlbum(String album) { this.album = album; }
    public void setYear(int year) { this.year = year; } // Setter cho year
}