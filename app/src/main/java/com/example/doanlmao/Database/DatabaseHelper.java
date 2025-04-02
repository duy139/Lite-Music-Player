package com.example.doanlmao.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.doanlmao.Model.Song;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Tên cơ sở dữ liệu
    private static final String DATABASE_NAME = "MusicDB";
    // Phiên bản cơ sở dữ liệu (tăng lên 2 vì thêm cột mới)
    private static final int DATABASE_VERSION = 2;

    // Tên bảng và các cột
    private static final String TABLE_SONGS = "songs";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_ARTIST = "artist";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_ARTWORK = "artwork";
    private static final String COLUMN_ALBUM = "album";
    private static final String COLUMN_YEAR = "year"; // Thêm cột year

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng songs với các cột tương ứng
        String CREATE_SONGS_TABLE = "CREATE TABLE " + TABLE_SONGS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_ARTIST + " TEXT, " +
                COLUMN_DURATION + " TEXT, " +
                COLUMN_PATH + " TEXT UNIQUE, " +  // Đảm bảo đường dẫn bài hát là duy nhất
                COLUMN_ARTWORK + " BLOB, " +
                COLUMN_ALBUM + " TEXT, " +
                COLUMN_YEAR + " INTEGER)"; // Thêm cột year
        db.execSQL(CREATE_SONGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Thêm cột year vào bảng hiện có mà không xóa dữ liệu
            db.execSQL("ALTER TABLE " + TABLE_SONGS + " ADD COLUMN " + COLUMN_YEAR + " INTEGER DEFAULT 0");
        }
        // Nếu có các phiên bản nâng cấp khác trong tương lai, thêm logic ở đây
    }

    // Thêm hoặc cập nhật bài hát vào cơ sở dữ liệu
    public void addOrUpdateSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TITLE, song.getTitle());
        values.put(COLUMN_ARTIST, song.getArtist());
        values.put(COLUMN_DURATION, song.getDuration());
        values.put(COLUMN_PATH, song.getPath());
        values.put(COLUMN_ARTWORK, song.getArtwork());
        values.put(COLUMN_ALBUM, song.getAlbum());
        values.put(COLUMN_YEAR, song.getYear()); // Lưu year

        long result = db.insertWithOnConflict(TABLE_SONGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d("DatabaseHelper", "Inserted/Updated song: " + song.getTitle() + ", result: " + result);
        db.close();
    }

    // Xóa bài hát khỏi cơ sở dữ liệu theo đường dẫn
    public void deleteSong(String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SONGS, COLUMN_PATH + "=?", new String[]{path});
        db.close();
    }

    // Lấy danh sách tất cả bài hát từ cơ sở dữ liệu
    public List<Song> getAllSongs() {
        List<Song> songList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SONGS, null, null, null, null, null, COLUMN_TITLE + " ASC");
        Log.d("DatabaseHelper", "Total songs in database when queried: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Song song = new Song(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTIST)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DURATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATH)),
                        cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_ARTWORK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALBUM)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEAR)) // Lấy year
                );
                songList.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return songList;
    }

    public void clearSongs() {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.w("DatabaseHelper", "Clearing all songs from database");
        db.execSQL("DELETE FROM " + TABLE_SONGS);
        db.close();
    }
}