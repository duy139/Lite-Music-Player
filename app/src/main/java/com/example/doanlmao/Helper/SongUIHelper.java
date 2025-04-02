package com.example.doanlmao.Helper;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.example.doanlmao.Misc.SongSorter;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SongUIHelper {
    private final Context context;
    private ImageView coverPreview;
    private Uri selectedAudioUri;
    private TextView audioFileNameTextView;
    private Uri selectedCoverUri;
    private TextView selectedAudioName;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final SongFragmentCallbacks callbacks;

    public interface SongFragmentCallbacks {
        void onSaveSong(String title, String artist, String album, int year, Uri audioUri, Uri coverUri);
        void onDeleteSong(Song song, int position);
        void onRefreshSongs();
        void onAddNewSong(String title, String artist, String album, int year, Uri audioUri, Uri coverUri); // Đảm bảo có phương thức này
    }

    public SongUIHelper(Context context, SongFragmentCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
    }

    // Hiển thị dialog tùy chọn khi ấn giữ bài hát
    public void showSongOptionsDialog(Song song, int position, ActivityResultLauncher<String> imagePickerLauncher) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(song.getTitle());
        builder.setItems(new String[]{"Thông tin bài hát", "Chỉnh sửa thông tin", "Xóa"}, (dialog, which) -> {
            switch (which) {
                case 0: // Thông tin bài hát
                    showSongInfoDialog(song);
                    break;
                case 1: // Chỉnh sửa
                    showEditSongDialog(imagePickerLauncher, song);
                    break;
                case 2: // Xóa
                    confirmDeleteSong(song, position);
                    break;
            }
        });
        builder.show();
    }

    private void showSongInfoDialog(Song song) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_song_info, null);
        dialog.setContentView(dialogView);

        TextView songTitleText = dialogView.findViewById(R.id.song_title_text);
        TextView artistText = dialogView.findViewById(R.id.artist_text);
        TextView albumText = dialogView.findViewById(R.id.album_text);
        TextView yearText = dialogView.findViewById(R.id.year_text);
        TextView nameText = dialogView.findViewById(R.id.name_text);
        TextView modifiedText = dialogView.findViewById(R.id.modified_text);
        TextView durationText = dialogView.findViewById(R.id.duration_text);
        TextView typeText = dialogView.findViewById(R.id.type_text);
        TextView pathText = dialogView.findViewById(R.id.path_text);
        ImageButton closeButton = dialogView.findViewById(R.id.close_button);

        // Lấy thông tin bài hát
        String songTitle = song.getTitle();
        String artist = song.getArtist() != null ? song.getArtist() : "Không xác định";
        String album = song.getAlbum() != null ? song.getAlbum() : "Không xác định";
        String year = song.getYear() > 0 ? String.valueOf(song.getYear()) : "Không xác định";
        String fileName = song.getTitle();
        String duration = song.getDuration();
        String path = song.getPath();
        String fileType = getFileType(Uri.parse(path));
        String lastModified = "Không xác định";

        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(context, Uri.parse(path));
            if (documentFile != null) {
                fileName = documentFile.getName();
                lastModified = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(new Date(documentFile.lastModified()));
            }
        } catch (Exception e) {
            Log.e("SongUIHelper", "Error getting file info", e);
        }

        songTitleText.setText("Tên bài hát: " + songTitle);
        artistText.setText("Nghệ sĩ: " + artist);
        albumText.setText("Album: " + album);
        yearText.setText("Năm sáng tác: " + year);
        nameText.setText("Tên file: " + fileName);
        modifiedText.setText("Thời gian chỉnh sửa: " + lastModified);
        durationText.setText("Thời lượng: " + duration);
        typeText.setText("Loại file: " + fileType);
        pathText.setText("Đường dẫn: " + path);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Hàm lấy loại file từ URI
    private String getFileType(Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                switch (mimeType) {
                    case "audio/mpeg":
                        return "Audio (MP3)";
                    case "audio/flac":
                        return "Audio (FLAC)";
                    case "audio/wav":
                    case "audio/x-wav":
                        return "Audio (WAV)";
                    default:
                        return "Audio (" + mimeType + ")";
                }
            }
        } catch (Exception e) {
            Log.e("SongUIHelper", "Error getting file type", e);
        }
        return "Không xác định";
    }

    // Hiển thị dialog chỉnh sửa bài hát
    public void showEditSongDialog(ActivityResultLauncher<String> imagePickerLauncher, Song song) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_song, null);
        builder.setView(dialogView);

        TextInputEditText titleInput = dialogView.findViewById(R.id.title_input);
        TextInputEditText artistInput = dialogView.findViewById(R.id.artist_input);
        TextInputEditText albumInput = dialogView.findViewById(R.id.album_input);
        TextInputEditText yearInput = dialogView.findViewById(R.id.year_input);
        MaterialButton selectCoverButton = dialogView.findViewById(R.id.select_cover_button);
        coverPreview = dialogView.findViewById(R.id.cover_preview);
        MaterialButton saveButton = dialogView.findViewById(R.id.save_button);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancel_button);

        if (song != null) {
            titleInput.setText(song.getTitle());
            artistInput.setText(song.getArtist());
            albumInput.setText(song.getAlbum());
            yearInput.setText(String.valueOf(song.getYear()));
            selectedAudioUri = Uri.parse(song.getPath());
            if (song.getArtwork() != null && song.getArtwork().length > 0) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(song.getArtwork(), 0, song.getArtwork().length);
                    coverPreview.setImageBitmap(bitmap);
                    coverPreview.setVisibility(View.VISIBLE); // Hiển thị ảnh bìa
                } catch (Exception e) {
                    Log.e("SongUIHelper", "Error loading artwork", e);
                    coverPreview.setVisibility(View.GONE);
                }
            } else {
                coverPreview.setVisibility(View.GONE);
            }
        }

        selectCoverButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String artist = artistInput.getText().toString().trim();
            String album = albumInput.getText().toString().trim();
            String yearStr = yearInput.getText().toString().trim();
            int year = 0;
            if (!yearStr.isEmpty()) {
                try {
                    year = Integer.parseInt(yearStr);
                    if (year < 0 || year > 9999) {
                        Toast.makeText(context, "Năm không hợp lệ (0-9999)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Năm phải là số", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (selectedAudioUri == null) {
                Toast.makeText(context, "Không tìm thấy file nhạc", Toast.LENGTH_SHORT).show();
                return;
            }
            if (title.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            callbacks.onSaveSong(title, artist, album, year, selectedAudioUri, selectedCoverUri);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    // Xác nhận xóa bài hát
    private void confirmDeleteSong(Song song, int position) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Xóa bài hát")
                .setMessage("Bạn có chắc muốn xóa " + song.getTitle() + "?")
                .setPositiveButton("Xóa", (dialog, which) -> callbacks.onDeleteSong(song, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Hiển thị dialog "More" (Thêm bài hát mới, Quét lại thư mục và Sắp xếp theo)
    public void showMoreOptionsDialog(Runnable onRescan, Runnable onSort, Runnable onAddNewSong) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Tùy chọn");
        builder.setItems(new String[]{"Thêm bài hát mới", "Quét lại thư mục", "Sắp xếp theo"}, (dialog, which) -> {
            switch (which) {
                case 0: // Thêm bài hát mới
                    onAddNewSong.run();
                    break;
                case 1: // Quét lại thư mục
                    onRescan.run();
                    break;
                case 2: // Sắp xếp theo
                    onSort.run();
                    break;
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }
    // Hiển thị dialog thêm bài hát mới
    public void showAddSongDialog(ActivityResultLauncher<String> audioPickerLauncher, ActivityResultLauncher<String> imagePickerLauncher) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_song, null);
        builder.setView(dialogView);

        TextInputEditText titleInput = dialogView.findViewById(R.id.title_input);
        TextInputEditText artistInput = dialogView.findViewById(R.id.artist_input);
        TextInputEditText albumInput = dialogView.findViewById(R.id.album_input);
        TextInputEditText yearInput = dialogView.findViewById(R.id.year_input);
        MaterialButton selectAudioButton = dialogView.findViewById(R.id.select_audio_button);
        MaterialButton selectCoverButton = dialogView.findViewById(R.id.select_cover_button);
        coverPreview = dialogView.findViewById(R.id.cover_preview);
        audioFileNameTextView = dialogView.findViewById(R.id.audio_file_name); // Khởi tạo TextView
        MaterialButton saveButton = dialogView.findViewById(R.id.save_button);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancel_button);

        // Reset các giá trị
        selectedAudioUri = null;
        selectedCoverUri = null;
        coverPreview.setVisibility(View.GONE);
        audioFileNameTextView.setText("Chưa chọn file nhạc");

        selectAudioButton.setOnClickListener(v -> audioPickerLauncher.launch("audio/*"));

        selectCoverButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String artist = artistInput.getText().toString().trim();
            String album = albumInput.getText().toString().trim();
            String yearStr = yearInput.getText().toString().trim();
            int year = 0;

            if (selectedAudioUri == null) {
                Toast.makeText(context, "Vui lòng chọn file nhạc", Toast.LENGTH_SHORT).show();
                return;
            }

            if (title.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!yearStr.isEmpty()) {
                try {
                    year = Integer.parseInt(yearStr);
                    if (year < 0 || year > 9999) {
                        Toast.makeText(context, "Năm không hợp lệ (0-9999)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Năm phải là số", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            callbacks.onAddNewSong(title, artist, album, year, selectedAudioUri, selectedCoverUri);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public void handleAudioPicked(Uri uri) {
        if (uri != null) {
            selectedAudioUri = uri;
            // Lấy tên file từ Uri và hiển thị
            String fileName = getFileNameFromUri(uri);
            if (fileName != null) {
                audioFileNameTextView.setText(fileName);
            } else {
                audioFileNameTextView.setText("Không xác định tên file");
            }
        }
    }

    // Lấy tên file từ Uri
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Log.e("SongUIHelper", "Error getting file name from URI: " + uri, e);
        }
        return fileName;
    }

    public void handleImagePicked(Uri uri) {
        if (uri != null) {
            selectedCoverUri = uri;
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    if (coverPreview != null) {
                        coverPreview.setImageBitmap(bitmap);
                        coverPreview.setVisibility(View.VISIBLE);
                    }
                }
            } catch (Exception e) {
                Toast.makeText(context, "Lỗi khi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                coverPreview.setVisibility(View.GONE);
            }
        }
    }



    // Hiển thị dialog sắp xếp (bottom sheet)
    public void showSortDialog(SongSorter songSorter, Runnable onSortSelected) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sort_options, null);
        dialog.setContentView(dialogView);

        RadioGroup sortTitleGroup = dialogView.findViewById(R.id.sort_title_group);
        RadioButton sortTitleAz = dialogView.findViewById(R.id.sort_title_az);
        RadioButton sortTitleZa = dialogView.findViewById(R.id.sort_title_za);

        RadioGroup sortArtistGroup = dialogView.findViewById(R.id.sort_artist_group);
        RadioButton sortArtistAz = dialogView.findViewById(R.id.sort_artist_az);
        RadioButton sortArtistZa = dialogView.findViewById(R.id.sort_artist_za);

        RadioGroup sortYearGroup = dialogView.findViewById(R.id.sort_year_group);
        RadioButton sortYearAsc = dialogView.findViewById(R.id.sort_year_asc);
        RadioButton sortYearDesc = dialogView.findViewById(R.id.sort_year_desc);

        RadioGroup sortDurationGroup = dialogView.findViewById(R.id.sort_duration_group);
        RadioButton sortDurationAsc = dialogView.findViewById(R.id.sort_duration_asc);
        RadioButton sortDurationDesc = dialogView.findViewById(R.id.sort_duration_desc);

        ImageButton closeButton = dialogView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Đặt trạng thái mặc định dựa trên kiểu sắp xếp cuối cùng
        int lastSortType = songSorter.getLastSortType();
        boolean lastSortAscending = songSorter.getLastSortAscending();

        if (lastSortType == SongSorter.SORT_BY_TITLE) {
            if (lastSortAscending) sortTitleAz.setChecked(true);
            else sortTitleZa.setChecked(true);
        } else if (lastSortType == SongSorter.SORT_BY_ARTIST) {
            if (lastSortAscending) sortArtistAz.setChecked(true);
            else sortArtistZa.setChecked(true);
        } else if (lastSortType == SongSorter.SORT_BY_YEAR) {
            if (lastSortAscending) sortYearAsc.setChecked(true);
            else sortYearDesc.setChecked(true);
        } else if (lastSortType == SongSorter.SORT_BY_DURATION) {
            if (lastSortAscending) sortDurationAsc.setChecked(true);
            else sortDurationDesc.setChecked(true);
        }

        sortTitleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_title_az) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_TITLE, true);
            } else if (checkedId == R.id.sort_title_za) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_TITLE, false);
            }
            onSortSelected.run();
            dialog.dismiss();
        });

        sortArtistGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_artist_az) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_ARTIST, true);
            } else if (checkedId == R.id.sort_artist_za) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_ARTIST, false);
            }
            onSortSelected.run();
            dialog.dismiss();
        });

        sortYearGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_year_asc) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_YEAR, true);
            } else if (checkedId == R.id.sort_year_desc) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_YEAR, false);
            }
            onSortSelected.run();
            dialog.dismiss();
        });

        sortDurationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_duration_asc) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_DURATION, true);
            } else if (checkedId == R.id.sort_duration_desc) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_DURATION, false);
            }
            onSortSelected.run();
            dialog.dismiss();
        });

        dialog.show();
    }



    public void setSelectedCoverUri(Uri uri) {
        this.selectedCoverUri = uri;
    }

    public void setCoverPreview(Bitmap bitmap) {
        if (coverPreview != null) {
            coverPreview.setImageBitmap(bitmap);
        }
    }
}
