package com.example.doanlmao.Model;

public class Playlist {
    private long id;
    private String name;
    private String note;
    private byte[] coverImage;

    public Playlist(long id, String name, String note, byte[] coverImage) {
        this.id = id;
        this.name = name;
        this.note = note;
        this.coverImage = coverImage;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNote() {
        return note;
    }

    public byte[] getCoverImage() {
        return coverImage;
    }
}