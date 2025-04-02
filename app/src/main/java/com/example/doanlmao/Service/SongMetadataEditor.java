package com.example.doanlmao.Service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.doanlmao.Model.Song;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SongMetadataEditor {
    private final Context context;
    private static final String TAG = "SongMetadataEditor";

    public SongMetadataEditor(Context context) {
        this.context = context;
    }

    public void updateMetadata(Song song, String title, String artist, String album, Uri coverUri) {
        updateMetadata(Uri.parse(song.getPath()), title, artist, album, song.getYear(), coverUri);
    }

    public void updateMetadata(Uri audioUri, String title, String artist, String album, int year, Uri coverUri) {
        try {
            // Xác định định dạng file
            String mimeType = context.getContentResolver().getType(audioUri);
            String fileExtension;
            if (mimeType != null) {
                switch (mimeType) {
                    case "audio/flac":
                        fileExtension = ".flac";
                        break;
                    case "audio/wav":
                    case "audio/x-wav":
                        fileExtension = ".wav";
                        break;
                    case "audio/mpeg":
                    default:
                        fileExtension = ".mp3";
                        break;
                }
            } else {
                fileExtension = ".mp3"; // Mặc định nếu không xác định được
            }

            // Tạo file tạm với đuôi đúng
            File tempFile = new File(context.getCacheDir(), "temp_audio_" + System.currentTimeMillis() + fileExtension);
            try (InputStream inputStream = context.getContentResolver().openInputStream(audioUri);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                if (inputStream == null) throw new IOException("Không thể mở InputStream từ URI: " + audioUri);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            AudioFile audioFile = AudioFileIO.read(tempFile);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            tag.setField(FieldKey.TITLE, title);
            tag.setField(FieldKey.ARTIST, artist);
            tag.setField(FieldKey.ALBUM, album);
            if (year > 0) {
                tag.setField(FieldKey.YEAR, String.valueOf(year));
            }

            if (coverUri != null) {
                try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(coverUri, "r")) {
                    if (pfd != null) {
                        FileDescriptor fd = pfd.getFileDescriptor();
                        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd);
                        Bitmap optimizedBitmap = optimizeBitmap(bitmap, 720, 720);
                        File tempCoverFile = createTempFileFromBitmap(optimizedBitmap, "cover_temp.jpg");
                        Artwork artwork = ArtworkFactory.createArtworkFromFile(tempCoverFile);
                        tag.deleteArtworkField();
                        tag.setField(artwork);
                        tempCoverFile.delete();
                    }
                }
            }

            audioFile.commit();

            try (FileInputStream inputStream = new FileInputStream(tempFile);
                 OutputStream outputStream = context.getContentResolver().openOutputStream(audioUri, "rwt")) {
                if (outputStream == null) throw new IOException("Không thể mở OutputStream để ghi file gốc: " + audioUri);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            tempFile.delete();
            Log.d(TAG, "Updated metadata for song: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Error updating song metadata", e);
            throw new RuntimeException("Lỗi khi cập nhật thông tin: " + e.getMessage());
        }
    }


    private Bitmap optimizeBitmap(Bitmap original, int targetWidth, int targetHeight) {
        int width = original.getWidth();
        int height = original.getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        Bitmap cropped = Bitmap.createBitmap(original, x, y, size, size);
        Bitmap resized = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true);
        if (cropped != original) cropped.recycle();
        original.recycle();
        return resized;
    }

    private File createTempFileFromBitmap(Bitmap bitmap, String fileName) throws Exception {
        File tempFile = new File(context.getCacheDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        }
        return tempFile;
    }
}