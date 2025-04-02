package com.example.doanlmao.Service;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Helper.DriveServiceHelper;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.media.MediaMetadataRetriever;

public class BackupRestoreManager {
    private final Context context;
    private final DriveServiceHelper driveServiceHelper;
    private final SongScanner songScanner;

    public BackupRestoreManager(Context context, DriveServiceHelper driveServiceHelper) {
        this.context = context;
        this.driveServiceHelper = driveServiceHelper;
        this.songScanner = new SongScanner(context, new DatabaseHelper(context));
    }

    public void backupSongs(List<Song> songs, Runnable onComplete) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(context)
                .setView(R.layout.dialog_progress)
                .setTitle("Đang sao lưu nhạc lên Google Drive...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            for (Song song : songs) {
                try {
                    File tempFile = createTempFileFromUri(Uri.parse(song.getPath()), song.getTitle());
                    if (tempFile != null) {
                        String fileName = getFileNameFromMetadata(tempFile, song.getTitle());
                        File fileToUpload = new File(context.getCacheDir(), fileName);
                        if (!tempFile.renameTo(fileToUpload)) {
                            fileToUpload = tempFile;
                        }
                        final File finalFileToUpload = fileToUpload;
                        driveServiceHelper.createFile(finalFileToUpload, "audio/mpeg")
                                .addOnSuccessListener(fileId -> {
                                    Log.d("BackupRestoreManager", "Uploaded: " + fileName + " with ID: " + fileId);
                                    finalFileToUpload.delete();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("BackupRestoreManager", "Upload failed: " + e.getMessage());
                                    finalFileToUpload.delete();
                                });
                    }
                } catch (Exception e) {
                    Log.e("BackupRestoreManager", "Error processing song: " + song.getTitle(), e);
                }
            }
            ((Activity) context).runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(context, "Đã sao lưu " + songs.size() + " bài hát!", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
            });
        }).start();
    }

    private String getFileNameFromMetadata(File file, String fallbackName) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            retriever.release();

            if (title != null && !title.isEmpty() && artist != null && !artist.isEmpty()) {
                return title + " - " + artist + ".mp3";
            } else if (title != null && !title.isEmpty()) {
                return title + ".mp3";
            } else if (artist != null && !artist.isEmpty()) {
                return artist + " - " + fallbackName + ".mp3";
            }
        } catch (Exception e) {
            Log.e("BackupRestoreManager", "Error extracting metadata: " + e.getMessage());
        }
        return fallbackName.replaceAll("[<>:\"/\\\\|?*]", "_") + ".mp3";
    }

    public void restoreSongs(Runnable onComplete) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(context)
                .setView(R.layout.dialog_progress)
                .setTitle("Đang tải danh sách file từ Google Drive...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        driveServiceHelper.listAudioFiles().addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                List<com.google.api.services.drive.model.File> driveFiles = task.getResult();
                if (driveFiles.isEmpty()) {
                    Toast.makeText(context, "Không có file audio để khôi phục!", Toast.LENGTH_SHORT).show();
                    if (onComplete != null) onComplete.run();
                    return;
                }

                String[] fileNames = driveFiles.stream()
                        .map(file -> file.getName())
                        .toArray(String[]::new);
                boolean[] checkedItems = new boolean[driveFiles.size()];
                List<com.google.api.services.drive.model.File> selectedFiles = new ArrayList<>();

                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context)
                        .setTitle("Chọn file audio để khôi phục")
                        .setMultiChoiceItems(fileNames, checkedItems, (dialog, which, isChecked) -> {
                            if (isChecked) {
                                selectedFiles.add(driveFiles.get(which));
                            } else {
                                selectedFiles.remove(driveFiles.get(which));
                            }
                        })
                        .setPositiveButton("Khôi phục", (dialog, which) -> {
                            if (selectedFiles.isEmpty()) {
                                Toast.makeText(context, "Chưa chọn file nào!", Toast.LENGTH_SHORT).show();
                            } else {
                                downloadSelectedFiles(selectedFiles, onComplete);
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .setNeutralButton("Xóa", (dialog, which) -> {
                            if (selectedFiles.isEmpty()) {
                                Toast.makeText(context, "Chưa chọn file nào để xóa!", Toast.LENGTH_SHORT).show();
                            } else {
                                // Dialog xác nhận xóa
                                new MaterialAlertDialogBuilder(context)
                                        .setTitle("Xác nhận xóa")
                                        .setMessage("Bạn có chắc muốn xóa " + selectedFiles.size() + " file đã chọn trên Google Drive?")
                                        .setPositiveButton("Có", (confirmDialog, confirmWhich) -> {
                                            deleteSelectedFiles(selectedFiles, onComplete);
                                        })
                                        .setNegativeButton("Không", null)
                                        .show();
                            }
                        });
                dialogBuilder.show();
            } else {
                Log.e("BackupRestoreManager", "Error listing files: " + task.getException().getMessage(), task.getException());
                Toast.makeText(context, "Lỗi tải danh sách: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void downloadSelectedFiles(List<com.google.api.services.drive.model.File> files, Runnable onComplete) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(context)
                .setView(R.layout.dialog_progress)
                .setTitle("Đang khôi phục...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            Uri treeUri = songScanner.loadTreeUri();
            if (treeUri == null || !songScanner.hasUriPermission(treeUri)) {
                Log.e("BackupRestoreManager", "No valid treeUri for saving files");
                ((Activity) context).runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Chưa chọn thư mục lưu nhạc!", Toast.LENGTH_LONG).show();
                    if (onComplete != null) onComplete.run();
                });
                return;
            }

            DocumentFile treeDir = DocumentFile.fromTreeUri(context, treeUri);
            if (treeDir == null || !treeDir.isDirectory()) {
                Log.e("BackupRestoreManager", "Invalid treeDir: " + treeUri);
                ((Activity) context).runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Thư mục lưu nhạc không hợp lệ!", Toast.LENGTH_LONG).show();
                    if (onComplete != null) onComplete.run();
                });
                return;
            }

            for (com.google.api.services.drive.model.File driveFile : files) {
                String fileName = driveFile.getName();
                DocumentFile existingFile = treeDir.findFile(fileName);
                if (existingFile == null || !existingFile.exists()) {
                    DocumentFile newFile = treeDir.createFile(driveFile.getMimeType(), fileName);
                    if (newFile != null) {
                        try (OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri())) {
                            driveServiceHelper.downloadFile(driveFile.getId(), out);
                            out.flush();
                            Log.d("BackupRestoreManager", "Downloaded: " + fileName);
                            songScanner.scanFolderAsync(treeUri, songs -> {});
                        } catch (IOException e) {
                            Log.e("BackupRestoreManager", "Error downloading file: " + fileName + ", " + e.getMessage());
                        }
                    } else {
                        Log.e("BackupRestoreManager", "Failed to create file: " + fileName);
                    }
                } else {
                    Log.d("BackupRestoreManager", "Skipped: " + fileName + " already exists");
                }
            }
            ((Activity) context).runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(context, "Đã khôi phục " + files.size() + " file!", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
            });
        }).start();
    }

    private void deleteSelectedFiles(List<com.google.api.services.drive.model.File> files, Runnable onComplete) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(context)
                .setView(R.layout.dialog_progress)
                .setTitle("Đang xóa file...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            for (com.google.api.services.drive.model.File driveFile : files) {
                driveServiceHelper.deleteFile(driveFile.getId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d("BackupRestoreManager", "Deleted: " + driveFile.getName());
                        })
                        .addOnFailureListener(e -> {
                            Log.e("BackupRestoreManager", "Delete failed: " + driveFile.getName() + ", " + e.getMessage());
                        });
            }
            ((Activity) context).runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(context, "Đã xóa " + files.size() + " file trên Google Drive!", Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
                // Gọi lại restoreSongs để cập nhật danh sách
                restoreSongs(onComplete);
            });
        }).start();
    }

    private File createTempFileFromUri(Uri uri, String fileName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                String safeFileName = fileName.replaceAll("[<>:\"/\\\\|?*]", "_") + ".mp3";
                File tempFile = new File(context.getCacheDir(), safeFileName);
                try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                inputStream.close();
                return tempFile;
            }
        } catch (IOException e) {
            Log.e("BackupRestoreManager", "Error creating temp file from URI: " + uri, e);
        }
        return null;
    }
}