package com.example.doanlmao.Service;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.documentfile.provider.DocumentFile;

import com.example.doanlmao.Model.Song;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class SongFileManager {
    private static final String TAG = "SongFileManager";
    private final Context context;
    private final Logger logger = Logger.getLogger(SongFileManager.class.getName());

    public SongFileManager(Context context) {
        this.context = context;
    }

    public void updateFileMetadata(Uri fileUri, String title, String artist, String album, int year, Uri coverUri) throws Exception {
        File tempFile = createTempFileFromUri(fileUri);
        if (tempFile == null) {
            throw new Exception("Không thể tạo file tạm để cập nhật metadata");
        }

        try {
            AudioFile audioFile = AudioFileIO.read(tempFile);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();

            if (title != null && !title.isEmpty()) tag.setField(FieldKey.TITLE, title);
            if (artist != null && !artist.isEmpty()) tag.setField(FieldKey.ARTIST, artist);
            if (album != null && !album.isEmpty()) tag.setField(FieldKey.ALBUM, album);
            if (year > 0) tag.setField(FieldKey.YEAR, String.valueOf(year));

            if (coverUri != null) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(coverUri)) {
                    if (inputStream != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        // Resize về tối đa 720x720
                        int maxSize = 720;
                        if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                            float scale = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                            int newWidth = Math.round(bitmap.getWidth() * scale);
                            int newHeight = Math.round(bitmap.getHeight() * scale);
                            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                        }
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
                        byte[] artworkData = byteArrayOutputStream.toByteArray();

                        Artwork artwork = ArtworkFactory.getNew();
                        artwork.setBinaryData(artworkData);
                        artwork.setMimeType("image/jpeg");
                        tag.deleteArtworkField();
                        tag.setField(artwork);
                        logger.info("Resized artwork to " + bitmap.getWidth() + "x" + bitmap.getHeight() + ", size: " + (artworkData.length / 1024) + "KB");
                    }
                }
            }

            audioFile.commit();

            DocumentFile documentFile = DocumentFile.fromSingleUri(context, fileUri);
            if (documentFile != null && documentFile.exists()) {
                try (OutputStream outputStream = context.getContentResolver().openOutputStream(fileUri)) {
                    if (outputStream != null) {
                        try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
            }
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    public Song createSongFromUri(Uri audioUri, String title, String artist, String album, int year, Uri coverUri) throws Exception {
        File tempFile = createTempFileFromUri(audioUri);
        if (tempFile == null) {
            throw new Exception("Không thể tạo file tạm để đọc metadata");
        }

        try {
            AudioFile audioFile = AudioFileIO.read(tempFile);
            Tag tag = audioFile.getTag();
            AudioHeader header = audioFile.getAudioHeader();

            String finalTitle = title;
            String finalArtist = artist;
            String finalAlbum = album;
            int finalYear = year;
            byte[] artwork = null;
            long durationMs = header.getTrackLength() * 1000L;

            if (tag != null) {
                if (finalTitle == null || finalTitle.isEmpty()) finalTitle = fixEncoding(tag.getFirst(FieldKey.TITLE));
                if (finalArtist == null || finalArtist.isEmpty()) finalArtist = fixEncoding(tag.getFirst(FieldKey.ARTIST));
                if (finalAlbum == null || finalAlbum.isEmpty()) finalAlbum = fixEncoding(tag.getFirst(FieldKey.ALBUM));
                if (finalYear == 0) {
                    String yearStr = tag.getFirst(FieldKey.YEAR);
                    if (yearStr != null && !yearStr.isEmpty()) {
                        if (yearStr.contains("-")) yearStr = yearStr.split("-")[0];
                        try {
                            finalYear = Integer.parseInt(yearStr);
                        } catch (NumberFormatException e) {
                            logger.warning("Invalid year format: " + yearStr);
                        }
                    }
                }
                Artwork artworkObj = tag.getFirstArtwork();
                if (artworkObj != null) artwork = artworkObj.getBinaryData();
            }

            if (coverUri != null) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(coverUri)) {
                    if (inputStream != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        // Resize về tối đa 720x720
                        int maxSize = 720;
                        if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                            float scale = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                            int newWidth = Math.round(bitmap.getWidth() * scale);
                            int newHeight = Math.round(bitmap.getHeight() * scale);
                            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                        }
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                        artwork = stream.toByteArray();
                        logger.info("Resized artwork to " + bitmap.getWidth() + "x" + bitmap.getHeight() + ", size: " + (artwork.length / 1024) + "KB");
                    }
                }
            }

            String fileName = getFileNameFromUri(audioUri);
            if (finalTitle == null || finalTitle.trim().isEmpty()) {
                finalTitle = fileName;
                if (finalTitle != null && finalTitle.contains(".")) {
                    finalTitle = finalTitle.substring(0, finalTitle.lastIndexOf("."));
                }
            }

            String duration = formatDuration(durationMs);

            return new Song(
                    finalTitle != null ? finalTitle : "Unknown Title",
                    finalArtist != null ? finalArtist : "Unknown Artist",
                    duration,
                    audioUri.toString(),
                    artwork,
                    finalAlbum != null ? finalAlbum : "Unknown Album",
                    finalYear
            );
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }
    public Uri moveFileToMusicFolder(Uri sourceUri, Uri treeUri) throws Exception {
        DocumentFile musicFolder = DocumentFile.fromTreeUri(context, treeUri);
        if (musicFolder == null || !musicFolder.isDirectory()) {
            logger.severe("Invalid Music folder URI: " + treeUri);
            return null;
        }

        String fileName = getFileNameFromUri(sourceUri);
        if (fileName == null) {
            logger.severe("Could not get file name from URI: " + sourceUri);
            return null;
        }

        DocumentFile existingFile = musicFolder.findFile(fileName);
        if (existingFile != null) {
            logger.warning("File already exists in Music folder: " + fileName);
            return null;
        }

        String mimeType = context.getContentResolver().getType(sourceUri);
        DocumentFile newFile = musicFolder.createFile(mimeType != null ? mimeType : "audio/mpeg", fileName);
        if (newFile == null) {
            logger.severe("Could not create file in Music folder: " + fileName);
            return null;
        }

        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = context.getContentResolver().openOutputStream(newFile.getUri())) {
            if (inputStream != null && outputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }

        DocumentFile sourceFile = DocumentFile.fromSingleUri(context, sourceUri);
        if (sourceFile != null && sourceFile.exists()) {
            boolean deleted = sourceFile.delete();
            if (!deleted) logger.warning("Could not delete source file: " + sourceUri);
        }

        return newFile.getUri();
    }

    public boolean deleteFile(Uri fileUri) {
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, fileUri);
            if (documentFile != null && documentFile.exists()) {
                return documentFile.delete();
            }
            return false;
        } catch (Exception e) {
            logger.severe("Error deleting file: " + e.getMessage());
            return false;
        }
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                String mimeType = context.getContentResolver().getType(uri);
                String extension = getExtensionFromMimeType(mimeType);
                File tempFile = new File(context.getCacheDir(), "temp_" + System.currentTimeMillis() + extension);
                try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                inputStream.close();
                logger.info("Created temp file: " + tempFile.getAbsolutePath());
                return tempFile;
            }
        } catch (Exception e) {
            logger.severe("Error creating temp file from URI: " + uri + " - " + e.getMessage());
        }
        return null;
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) fileName = cursor.getString(nameIndex);
            }
        } catch (Exception e) {
            logger.severe("Error getting file name from URI: " + uri + " - " + e.getMessage());
        }
        return fileName;
    }

    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return ".mp3";
        switch (mimeType) {
            case "audio/mpeg": return ".mp3";
            case "audio/flac": return ".flac";
            case "audio/wav":
            case "audio/x-wav": return ".wav";
            case "audio/x-m4a":
            case "audio/mp4": return ".m4a";
            case "audio/aac": return ".aac";
            default:
                logger.warning("Unknown MIME type: " + mimeType + ", defaulting to .mp3");
                return ".mp3";
        }
    }

    private String fixEncoding(String input) {
        if (input == null || input.isEmpty()) return input;
        try {
            if (!input.matches("^[\\p{L}\\p{N}\\p{P}\\p{Zs}]+$")) {
                byte[] bytes = input.getBytes("ISO-8859-1");
                String fixed = new String(bytes, "UTF-8");
                logger.info("Fixed encoding for: " + input + " -> " + fixed);
                return fixed;
            }
        } catch (Exception e) {
            logger.severe("Error fixing encoding for: " + input + " - " + e.getMessage());
        }
        return input;
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}