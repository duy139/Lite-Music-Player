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

public class FavoriteDatabase extends SQLiteOpenHelper {
    // Tên cơ sở dữ liệu và phiên bản
    private static final String DATABASE_NAME = "FavoriteSongs.db";
    private static final int DATABASE_VERSION = 2; // Tăng lên 2 vì thêm cột mới

    // Tên bảng và các cột
    private static final String TABLE_FAVORITES = "favorites";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_ARTIST = "artist";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_ALBUM = "album";
    private static final String COLUMN_ARTWORK = "artwork";
    private static final String COLUMN_YEAR = "year"; // Thêm cột year
    private static final String TAG = "FavoriteDatabase";

    // Đối tượng lock để đồng bộ hóa truy cập cơ sở dữ liệu
    private final Object lock = new Object();

    // Constructor
    public FavoriteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng favorites với các cột tương ứng
        String CREATE_TABLE = "CREATE TABLE " + TABLE_FAVORITES + " (" +
                COLUMN_PATH + " TEXT PRIMARY KEY," +
                COLUMN_TITLE + " TEXT," +
                COLUMN_ARTIST + " TEXT," +
                COLUMN_DURATION + " TEXT," +
                COLUMN_ALBUM + " TEXT," +
                COLUMN_ARTWORK + " BLOB," +
                COLUMN_YEAR + " INTEGER)"; // Thêm cột year
        db.execSQL(CREATE_TABLE);
        Log.d(TAG, "Favorite database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Thêm cột year vào bảng hiện có mà không xóa dữ liệu
            db.execSQL("ALTER TABLE " + TABLE_FAVORITES + " ADD COLUMN " + COLUMN_YEAR + " INTEGER DEFAULT 0");
        }
        // Nếu có các phiên bản nâng cấp khác trong tương lai, thêm logic ở đây
    }

    // Thêm bài hát vào danh sách yêu thích
    public void addFavorite(Song song) {
        SQLiteDatabase db = null;
        try {
            synchronized (lock) { // Đảm bảo chỉ có một luồng truy cập
                db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(COLUMN_PATH, song.getPath());
                values.put(COLUMN_TITLE, song.getTitle());
                values.put(COLUMN_ARTIST, song.getArtist());
                values.put(COLUMN_DURATION, song.getDuration());
                values.put(COLUMN_ALBUM, song.getAlbum());
                values.put(COLUMN_ARTWORK, song.getArtwork());
                values.put(COLUMN_YEAR, song.getYear()); // Lưu year

                // Chèn hoặc cập nhật bài hát nếu đã tồn tại
                db.insertWithOnConflict(TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                Log.d(TAG, "Added to favorites: " + song.getTitle());
            }
        } finally {
            if (db != null) db.close();
        }
    }

    // Xóa bài hát khỏi danh sách yêu thích theo đường dẫn
    public void removeFavorite(String path) {
        SQLiteDatabase db = null;
        try {
            synchronized (lock) {
                db = this.getWritableDatabase();
                db.delete(TABLE_FAVORITES, COLUMN_PATH + "=?", new String[]{path});
                Log.d(TAG, "Removed from favorites: " + path);
            }
        } finally {
            if (db != null) db.close();
        }
    }

    // Lấy danh sách tất cả bài hát yêu thích
    public List<Song> getAllFavorites() {
        List<Song> favoriteList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            synchronized (lock) {
                db = this.getReadableDatabase();
                cursor = db.query(TABLE_FAVORITES, null, null, null, null, null, COLUMN_TITLE + " ASC");

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        // Lấy thông tin bài hát từ cursor
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATH));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                        String artist = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTIST));
                        String duration = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
                        String album = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALBUM));
                        byte[] artwork = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_ARTWORK));
                        int year = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEAR)); // Lấy year

                        // Thêm bài hát vào danh sách yêu thích
                        Song song = new Song(title, artist, duration, path, artwork, album, year);
                        favoriteList.add(song);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        Log.d(TAG, "Loaded " + favoriteList.size() + " favorite songs");
        return favoriteList;
    }

    // Kiểm tra xem một bài hát có nằm trong danh sách yêu thích không
    public boolean isFavorite(String path) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            synchronized (lock) {
                db = this.getReadableDatabase();
                cursor = db.query(TABLE_FAVORITES, new String[]{COLUMN_PATH},
                        COLUMN_PATH + "=?", new String[]{path}, null, null, null);
                return cursor != null && cursor.getCount() > 0;
            }
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }
}