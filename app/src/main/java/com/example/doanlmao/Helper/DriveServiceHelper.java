package com.example.doanlmao.Helper;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class DriveServiceHelper {
    private final Drive mDriveService;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public DriveServiceHelper(Drive driveService) {
        this.mDriveService = driveService;
    }

    public Task<String> createFile(java.io.File file, String mimeType) {
        return Tasks.call(mExecutor, () -> {
            File fileMetadata = new File();
            fileMetadata.setName(file.getName());
            FileContent mediaContent = new FileContent(mimeType, file);
            File uploadedFile = mDriveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            return uploadedFile.getId();
        });
    }

    public Task<List<File>> listAudioFiles() {
        return Tasks.call(mExecutor, () -> {
            Drive.Files.List request = mDriveService.files().list()
                    .setQ("mimeType contains 'audio/' and trashed=false")
                    .setSpaces("drive")
                    .setFields("files(id, name, mimeType)");
            return request.execute().getFiles();
        });
    }

    public void downloadFile(String fileId, OutputStream outputStream) throws IOException {
        mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
    }

    public Task<Void> deleteFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            mDriveService.files().delete(fileId).execute();
            return null;
        });
    }
}