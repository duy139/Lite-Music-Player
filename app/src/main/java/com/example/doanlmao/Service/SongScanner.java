package com.example.doanlmao.Service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;

import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SongScanner {
    private final Context context;
    private final DatabaseHelper dbHelper;
    private static final String TAG = "SongScanner";
    private static final String PREFS_NAME = "MusicPrefs";
    private static final String KEY_TREE_URI = "treeUri";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SongScanner(Context context, DatabaseHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    // Quét folder bất đồng bộ, trả kết quả qua callback
    public void scanFolderAsync(Uri treeUri, Consumer<List<Song>> callback) {
        Log.d(TAG, "Starting scanFolderAsync with treeUri: " + treeUri);
        executor.execute(() -> {
            List<Song> songList = new ArrayList<>();
            scanFolder(treeUri, songList);
            Log.d(TAG, "Scan completed, found " + songList.size() + " songs");
            if (callback != null) {
                callback.accept(songList);
            }
        });
    }

    // Quét folder đồng bộ
    public void scanFolder(Uri treeUri, List<Song> songList) {
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, treeUri);
        if (documentFile == null || !documentFile.isDirectory()) {
            Log.e(TAG, "Invalid folder URI: " + treeUri);
            return;
        }

        scanFolderRecursive(documentFile, songList);
    }

    private void scanFolderRecursive(DocumentFile folder, List<Song> songList) {
        DocumentFile[] files = folder.listFiles();
        if (files == null) {
            Log.w(TAG, "No files found in folder: " + folder.getUri());
            return;
        }

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                scanFolderRecursive(file, songList);
            } else if (file.getName() != null && isAudioFile(file.getName())) {
                try {
                    Uri fileUri = file.getUri();
                    File tempFile = createTempFileFromUri(fileUri, file.getName());
                    if (tempFile == null) {
                        Log.w(TAG, "Could not create temp file for " + file.getName() + ", skipping...");
                        continue;
                    }

                    AudioFile audioFile = AudioFileIO.read(tempFile);
                    Tag tag = audioFile.getTag();
                    AudioHeader header = audioFile.getAudioHeader();

                    String title = null;
                    String artist = null;
                    String album = null;
                    int year = 0;
                    byte[] artwork = null;
                    long durationMs = header.getTrackLength() * 1000L;

                    if (tag != null) {
                        title = fixEncoding(tag.getFirst(FieldKey.TITLE));
                        artist = fixEncoding(tag.getFirst(FieldKey.ARTIST));
                        album = fixEncoding(tag.getFirst(FieldKey.ALBUM));
                        // Nếu artist là chuỗi rỗng, đặt thành null
                        if (artist != null && artist.trim().isEmpty()) {
                            artist = null;
                        }
                        // Nếu album là chuỗi rỗng, đặt thành null
                        if (album != null && album.trim().isEmpty()) {
                            album = null;
                        }
                        String yearStr = tag.getFirst(FieldKey.YEAR);
                        if (yearStr != null && !yearStr.isEmpty()) {
                            if (yearStr.contains("-")) {
                                yearStr = yearStr.split("-")[0];
                            }
                            try {
                                year = Integer.parseInt(yearStr);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "Invalid year format for " + file.getName() + ": " + yearStr);
                            }
                        }
                        Artwork artworkObj = tag.getFirstArtwork();
                        if (artworkObj != null) {
                            artwork = artworkObj.getBinaryData();
                        }
                    }

                    if (title == null || title.trim().isEmpty()) {
                        title = file.getName();
                        if (title != null && title.contains(".")) {
                            title = title.substring(0, title.lastIndexOf("."));
                        }
                    }

                    String duration = formatDuration(durationMs);

                    Song song = new Song(
                            title != null ? title : "Unknown Title",
                            artist != null ? artist : "Unknown Artist",
                            duration,
                            fileUri.toString(),
                            artwork,
                            album != null ? album : "Unknown Album",
                            year
                    );
                    dbHelper.addOrUpdateSong(song);
                    songList.add(song);
                    Log.d(TAG, "Added song to DB and list: " + song.getTitle() + " - URI: " + fileUri + " - Year: " + year);

                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error scanning file: " + file.getUri() + ", skipping...", e);
                }
            }
        }
    }


    // Hàm tạo file tạm từ Uri
    private File createTempFileFromUri(Uri uri, String fileName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                File tempFile = new File(context.getCacheDir(), "temp_" + fileName);
                try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                inputStream.close();
                return tempFile;
            } else {
                Log.w(TAG, "Could not open InputStream for URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating temp file from URI: " + uri, e);
        }
        return null;
    }
    // Hàm sửa encoding nếu cần
    private String fixEncoding(String input) {
        if (input == null || input.isEmpty()) return input;

        try {
            // Kiểm tra nếu chuỗi có ký tự không hợp lệ (có thể do encoding sai)
            if (!input.matches("^[\\p{L}\\p{N}\\p{P}\\p{Zs}]+$")) {
                // Thử chuyển từ ISO-8859-1 sang UTF-8
                byte[] bytes = input.getBytes("ISO-8859-1");
                String fixed = new String(bytes, "UTF-8");
                Log.d(TAG, "Fixed encoding for: " + input + " -> " + fixed);
                return fixed;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fixing encoding for: " + input, e);
        }
        return input;
    }

    // Kiểm tra và yêu cầu quyền truy cập folder
    public void checkAndRequestFolderAccess(ActivityResultLauncher<Intent> folderPickerLauncher) {
        Uri treeUri = loadTreeUri();
        if (treeUri == null || !hasUriPermission(treeUri)) {
            Log.d(TAG, "No valid treeUri, launching folder picker");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            folderPickerLauncher.launch(intent);
        } else {
            Log.d(TAG, "Valid treeUri found: " + treeUri);
        }
    }

    // Lưu treeUri
    public void saveTreeUri(Uri treeUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TREE_URI, treeUri.toString()).apply();
        Log.d(TAG, "Saved treeUri: " + treeUri);
    }

    // Load treeUri
    public Uri loadTreeUri() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_TREE_URI, null);
        return uriString != null ? Uri.parse(uriString) : null;
    }

    // Kiểm tra quyền của treeUri
    public boolean hasUriPermission(Uri uri) {
        if (uri == null) {
            Log.w(TAG, "URI is null, no permission");
            return false;
        }
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        for (UriPermission perm : permissions) {
            if (perm.getUri().equals(uri) && perm.isReadPermission()) {
                Log.d(TAG, "Permission granted for URI: " + uri + ", Read: " + perm.isReadPermission() + ", Write: " + perm.isWritePermission());
                return true;
            }
        }
        Log.w(TAG, "No persisted READ permission for URI: " + uri);
        return false;
    }

    private boolean isAudioFile(String fileName) {
        String[] audioExtensions = {".mp3", ".flac", ".wav", ".m4a", ".aac"};
        for (String ext : audioExtensions) {
            if (fileName.toLowerCase().endsWith(ext)) return true;
        }
        return false;
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // Đóng Executor khi không cần nữa
    public void shutdown() {
        executor.shutdown();
    }
}