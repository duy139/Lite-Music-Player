package com.example.doanlmao.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.doanlmao.Model.Playlist;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PlaylistDB";
    private static final int DATABASE_VERSION = 2;
    private static final String TAG = "PlaylistDatabase";

    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String COLUMN_PLAYLIST_ID = "id";
    private static final String COLUMN_PLAYLIST_NAME = "name";
    private static final String COLUMN_PLAYLIST_NOTE = "note";
    private static final String COLUMN_COVER_IMAGE = "cover_image";

    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SONG_ID = "song_id";

    public PlaylistDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PLAYLISTS_TABLE = "CREATE TABLE " + TABLE_PLAYLISTS + " (" +
                COLUMN_PLAYLIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PLAYLIST_NAME + " TEXT UNIQUE, " +
                COLUMN_PLAYLIST_NOTE + " TEXT, " +
                COLUMN_COVER_IMAGE + " BLOB)";
        db.execSQL(CREATE_PLAYLISTS_TABLE);

        String CREATE_PLAYLIST_SONGS_TABLE = "CREATE TABLE " + TABLE_PLAYLIST_SONGS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "playlist_id INTEGER, " +
                COLUMN_SONG_ID + " TEXT, " +
                "FOREIGN KEY (playlist_id) REFERENCES " + TABLE_PLAYLISTS + "(" + COLUMN_PLAYLIST_ID + ") ON DELETE CASCADE)";
        db.execSQL(CREATE_PLAYLIST_SONGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        onCreate(db);
    }

    public long addPlaylist(String playlistName, String note, byte[] coverImage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PLAYLIST_NAME, playlistName);
        values.put(COLUMN_PLAYLIST_NOTE, note);
        values.put(COLUMN_COVER_IMAGE, coverImage);
        long id = db.insert(TABLE_PLAYLISTS, null, values);
        db.close();
        Log.d(TAG, "Added playlist: " + playlistName + ", ID: " + id);
        return id;
    }

    public void deletePlaylist(long playlistId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PLAYLISTS, COLUMN_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
        db.close();
        Log.d(TAG, "Deleted playlist ID: " + playlistId);
    }

    public void updatePlaylist(long playlistId, String newName, String newNote, byte[] newCoverImage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PLAYLIST_NAME, newName);
        values.put(COLUMN_PLAYLIST_NOTE, newNote);
        values.put(COLUMN_COVER_IMAGE, newCoverImage);
        int rows = db.update(TABLE_PLAYLISTS, values, COLUMN_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
        db.close();
        Log.d(TAG, "Updated playlist ID: " + playlistId + ", rows affected: " + rows);
    }

    public List<Playlist> getAllPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_PLAYLIST_ID, COLUMN_PLAYLIST_NAME, COLUMN_PLAYLIST_NOTE, COLUMN_COVER_IMAGE};
        Cursor cursor = db.query(TABLE_PLAYLISTS, columns, null, null, null, null, COLUMN_PLAYLIST_NAME + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PLAYLIST_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYLIST_NAME));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLAYLIST_NOTE));
                byte[] coverImage = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_COVER_IMAGE));
                playlists.add(new Playlist(id, name, note, coverImage));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        Log.d(TAG, "Loaded " + playlists.size() + " playlists");
        return playlists;
    }

    public void addSongToPlaylist(long playlistId, String songPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("playlist_id", playlistId);
        values.put(COLUMN_SONG_ID, songPath);
        long result = db.insert(TABLE_PLAYLIST_SONGS, null, values);
        db.close();
        Log.d(TAG, "Added song " + songPath + " to playlist ID " + playlistId + ", result: " + result);
    }

    public void removeSongFromPlaylist(long playlistId, String songPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PLAYLIST_SONGS,
                "playlist_id=? AND " + COLUMN_SONG_ID + "=?",
                new String[]{String.valueOf(playlistId), songPath});
        db.close();
        Log.d(TAG, "Removed song " + songPath + " from playlist ID " + playlistId);
    }

    public List<String> getSongsInPlaylist(long playlistId) {
        List<String> songPaths = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLAYLIST_SONGS,
                new String[]{COLUMN_SONG_ID},
                "playlist_id=?",
                new String[]{String.valueOf(playlistId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SONG_ID));
                songPaths.add(path);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        Log.d(TAG, "Loaded " + songPaths.size() + " songs for playlist ID " + playlistId);
        return songPaths;
    }
    public void addPlaylistWithId(long id, String playlistName, String note, byte[] coverImage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PLAYLIST_ID, id); // Ghi đè ID từ Firebase
        values.put(COLUMN_PLAYLIST_NAME, playlistName);
        values.put(COLUMN_PLAYLIST_NOTE, note);
        values.put(COLUMN_COVER_IMAGE, coverImage);
        long result = db.insertWithOnConflict(TABLE_PLAYLISTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        Log.d(TAG, "Added playlist with ID " + id + ": " + playlistName + ", result: " + result);
    }

}

