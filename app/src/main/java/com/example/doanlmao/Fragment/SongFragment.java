package com.example.doanlmao.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Helper.SearchHelper;
import com.example.doanlmao.Helper.SongUIHelper;
import com.example.doanlmao.Misc.SongListManager;
import com.example.doanlmao.Misc.SongSorter;
import com.example.doanlmao.Model.MusicViewModel;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.example.doanlmao.Service.SongFileManager;
import com.example.doanlmao.Service.SongMetadataEditor;
import com.example.doanlmao.Service.SongScanner;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SongFragment extends Fragment implements SongUIHelper.SongFragmentCallbacks {
    private RecyclerView recyclerView;
    private SongListManager songListManager;
    private DatabaseHelper dbHelper;
    private static final String TAG = "SongFragment";
    private SongScanner songScanner;
    private SongUIHelper uiHelper;
    private AlertDialog progressDialog;
    private ActivityResultLauncher<Intent> folderPickerLauncher;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> audioPickerLauncher;
    private SongSorter songSorter;
    private SongFileManager songFileManager;
    private MusicViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new SongUIHelper(requireContext(), this);
        dbHelper = new DatabaseHelper(getContext());
        songScanner = new SongScanner(getContext(), dbHelper);
        songSorter = new SongSorter(getContext());
        songFileManager = new SongFileManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);

        folderPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri treeUri = result.getData().getData();
                if (treeUri != null) {
                    requireContext().getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    viewModel.setTreeUri(treeUri);
                    viewModel.scanSongs();
                }
            } else {
                Log.d(TAG, "Folder selection canceled");
                Toast.makeText(getContext(), "Vui lòng chọn thư mục để tiếp tục!", Toast.LENGTH_LONG).show();
                showFolderSelectionDialog();
            }
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) uiHelper.handleImagePicked(uri);
        });

        audioPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) uiHelper.handleAudioPicked(uri);
        });

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        songListManager = new SongListManager(getContext(), recyclerView);

        viewModel.getSongs().observe(getViewLifecycleOwner(), songs -> {
            songListManager.setSongList(songs);
            songListManager.applyLastSort();
            setupLongClickListener();
            Log.d(TAG, "Loaded " + songs.size() + " songs from ViewModel");
        });

        viewModel.isScanning().observe(getViewLifecycleOwner(), isScanning -> {
            if (isScanning) {
                showProgressDialog();
            } else {
                hideProgressDialog();
            }
        });

        Uri savedTreeUri = viewModel.getTreeUri();
        if (savedTreeUri == null || !songScanner.hasUriPermission(savedTreeUri)) {
            showFolderSelectionDialog();
        } else if (!viewModel.isScanning().getValue() && viewModel.getSongs().getValue().isEmpty()) {
            viewModel.scanSongs();
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && activity.miniPlayerController != null) {
                    if (dy > 0) { // Vuốt xuống
                        activity.miniPlayerController.setManuallyHidden(true);
                    } else if (dy < 0) { // Vuốt lên
                        activity.miniPlayerController.setManuallyHidden(false);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_more) {
            uiHelper.showMoreOptionsDialog(
                    this::refreshSongsInBackground,
                    () -> uiHelper.showSortDialog(songSorter, () -> {
                        int lastSortType = songSorter.getLastSortType();
                        boolean lastSortAscending = songSorter.getLastSortAscending();
                        songListManager.sortSongs(lastSortType, lastSortAscending);
                    }),
                    () -> uiHelper.showAddSongDialog(audioPickerLauncher, imagePickerLauncher)
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        songListManager.getSongAdapter().notifyDataSetChanged();
    }

    private void setupLongClickListener() {
        songListManager.getSongAdapter().setOnSongLongClickListener(position -> {
            Song song = songListManager.getFilteredSongList().get(position);
            uiHelper.showSongOptionsDialog(song, position, imagePickerLauncher);
        });
    }

    private void showFolderSelectionDialog() {
        songScanner.checkAndRequestFolderAccess(folderPickerLauncher);
    }

    private void showProgressDialog() {
        if (progressDialog == null || !progressDialog.isShowing()) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null);
            builder.setView(dialogView);
            builder.setMessage("Đang quét nhạc...");
            builder.setCancelable(false);
            progressDialog = builder.create();
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void refreshSongsInBackground() {
        viewModel.refreshSongs();
    }

    @Override
    public void onSaveSong(String title, String artist, String album, int year, Uri audioUri, Uri coverUri) {
        try {
            songFileManager.updateFileMetadata(audioUri, title, artist, album, year, coverUri);
            Song updatedSong = songFileManager.createSongFromUri(audioUri, title, artist, album, year, coverUri);
            viewModel.updateSong(audioUri, updatedSong);
            Toast.makeText(getContext(), "Đã cập nhật thông tin bài hát", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error updating song", e);
            Toast.makeText(getContext(), "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAddNewSong(String title, String artist, String album, int year, Uri audioUri, Uri coverUri) {
        try {
            Uri treeUri = viewModel.getTreeUri();
            if (treeUri == null || !songScanner.hasUriPermission(treeUri)) {
                Toast.makeText(getContext(), "Không có quyền truy cập thư mục Music", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri newFileUri = songFileManager.moveFileToMusicFolder(audioUri, treeUri);
            if (newFileUri == null) {
                Toast.makeText(getContext(), "Không thể di chuyển file vào thư mục Music", Toast.LENGTH_SHORT).show();
                return;
            }

            songFileManager.updateFileMetadata(newFileUri, title, artist, album, year, coverUri);
            Song newSong = songFileManager.createSongFromUri(newFileUri, title, artist, album, year, coverUri);
            if (newSong == null) {
                Toast.makeText(getContext(), "Không thể đọc file nhạc sau khi cập nhật", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.addSong(newSong);
            recyclerView.scrollToPosition(songListManager.getFilteredSongList().size() - 1);
            Toast.makeText(getContext(), "Đã thêm bài hát: " + newSong.getTitle(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error adding new song", e);
            Toast.makeText(getContext(), "Lỗi khi thêm bài hát: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDeleteSong(Song song, int position) {
        try {
            Uri fileUri = Uri.parse(song.getPath());
            if (songFileManager.deleteFile(fileUri)) {
                viewModel.deleteSong(song);
                Toast.makeText(getContext(), "Đã xóa " + song.getTitle(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Không thể xóa file: " + song.getTitle(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting song", e);
            Toast.makeText(getContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRefreshSongs() {
        refreshSongsInBackground();
    }
}