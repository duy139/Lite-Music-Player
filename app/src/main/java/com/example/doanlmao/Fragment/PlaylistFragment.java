package com.example.doanlmao.Fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Activity.PlaylistDetailsActivity;
import com.example.doanlmao.Adapter.PlaylistAdapter;
import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Database.FirebaseSyncManager;
import com.example.doanlmao.Database.PlaylistDatabase;
import com.example.doanlmao.Misc.SongSorter;
import com.example.doanlmao.Model.Playlist;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment {
    private RecyclerView recyclerView;
    private PlaylistAdapter playlistAdapter;
    private PlaylistDatabase playlistDb;
    private DatabaseHelper songDb;
    private static final String TAG = "PlaylistFragment";
    private ImageView coverPreview;
    private byte[] selectedCoverImage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private FirebaseSyncManager syncManager;
    private SongSorter songSorter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                    int maxSize = 720;
                    if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                        float scale = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                        int newWidth = Math.round(bitmap.getWidth() * scale);
                        int newHeight = Math.round(bitmap.getHeight() * scale);
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    }
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
                    selectedCoverImage = stream.toByteArray();
                    coverPreview.setImageBitmap(bitmap);
                    coverPreview.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Compressed image size: " + (selectedCoverImage.length / 1024) + "KB");
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Không thể tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null && activity.miniPlayerController != null) {
                    if (dy > 0) {
                        activity.miniPlayerController.setManuallyHidden(true);
                    } else if (dy < 0) {
                        activity.miniPlayerController.setManuallyHidden(false);
                    }
                }
            }
        });

        playlistDb = new PlaylistDatabase(getContext());
        songDb = new DatabaseHelper(getContext());
        syncManager = new FirebaseSyncManager(playlistDb);
        songSorter = new SongSorter(getContext());

        // Khởi tạo adapter một lần duy nhất
        List<Playlist> initialList = new ArrayList<>();
        playlistAdapter = new PlaylistAdapter(getContext(), initialList,
                playlist -> {
                    Intent intent = new Intent(getContext(), PlaylistDetailsActivity.class);
                    intent.putExtra("playlistId", playlist.getId());
                    intent.putExtra("playlistName", playlist.getName());
                    intent.putExtra("coverImage", playlist.getCoverImage());
                    intent.putExtra("note", playlist.getNote());
                    startActivity(intent);
                },
                playlist -> showEditOrDeleteDialog(playlist));
        recyclerView.setAdapter(playlistAdapter);

        loadPlaylists();

        syncManager.listenForPlaylistChanges(() -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    try {
                        loadPlaylists();
                    } catch (Exception e) {
                        Log.e(TAG, "Error refreshing playlists", e);
                    }
                });
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu_playlist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_more) {
            showOptionsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadPlaylists() {
        List<Playlist> playlists = playlistDb.getAllPlaylists();
        applyLastSort(playlists);
        playlistAdapter.updatePlaylists(playlists); // Cập nhật danh sách
        Log.d(TAG, "Loaded " + playlists.size() + " playlists");
    }

    private void applyLastSort(List<Playlist> playlists) {
        int lastSortType = songSorter.getLastSortType();
        boolean lastSortAscending = songSorter.getLastSortAscending();
        songSorter.sortPlaylists(playlists, lastSortType, lastSortAscending);
    }

    private void showOptionsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Tùy chọn");
        String[] options = {"Thêm playlist", "Sắp xếp"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showCreatePlaylistDialog();
            } else if (which == 1) {
                showSortDialog();
            }
        });
        builder.show();
    }

    private void showSortDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sort_options_playlist, null);
        dialog.setContentView(dialogView);

        RadioGroup sortTitleGroup = dialogView.findViewById(R.id.sort_title_group);
        RadioButton sortTitleAz = dialogView.findViewById(R.id.sort_title_az);
        RadioButton sortTitleZa = dialogView.findViewById(R.id.sort_title_za);

        ImageButton closeButton = dialogView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        int lastSortType = songSorter.getLastSortType();
        boolean lastSortAscending = songSorter.getLastSortAscending();

        if (lastSortType == SongSorter.SORT_BY_TITLE) {
            if (lastSortAscending) sortTitleAz.setChecked(true);
            else sortTitleZa.setChecked(true);
        }

        sortTitleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_title_az) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_TITLE, true);
            } else if (checkedId == R.id.sort_title_za) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_TITLE, false);
            }
            loadPlaylists(); // Tải lại danh sách sau khi sắp xếp
            dialog.dismiss();
        });

        dialog.show();
    }


    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_playlist, null);
        EditText editName = dialogView.findViewById(R.id.edit_playlist_name);
        EditText editNote = dialogView.findViewById(R.id.edit_playlist_note);
        coverPreview = dialogView.findViewById(R.id.cover_preview);
        Button btnSelectCover = dialogView.findViewById(R.id.select_cover_button);
        Button btnSave = dialogView.findViewById(R.id.save_button);
        Button btnCancel = dialogView.findViewById(R.id.cancel_button);

        selectedCoverImage = null;

        btnSelectCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String note = editNote.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập tên playlist!", Toast.LENGTH_SHORT).show();
                return;
            }
            long id = playlistDb.addPlaylist(name, note.isEmpty() ? null : note, selectedCoverImage);
            if (id != -1) {
                loadPlaylists();
                Toast.makeText(getContext(), "Đã tạo playlist '" + name + "' thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                syncManager.syncPlaylistsToFirebase(); // Chỉ sync khi user tạo
            } else {
                Toast.makeText(getContext(), "Playlist với tên '" + name + "' đã tồn tại!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEditOrDeleteDialog(Playlist playlist) {
        String[] options = {"Chỉnh sửa", "Xóa playlist"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(playlist.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditPlaylistDialog(playlist);
                    } else {
                        deletePlaylist(playlist);
                    }
                })
                .show();
    }

    private void showEditPlaylistDialog(Playlist playlist) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_playlist, null);
        EditText editName = dialogView.findViewById(R.id.edit_playlist_name);
        EditText editNote = dialogView.findViewById(R.id.edit_playlist_note);
        coverPreview = dialogView.findViewById(R.id.cover_preview);
        Button btnSelectCover = dialogView.findViewById(R.id.select_cover_button);
        Button btnSave = dialogView.findViewById(R.id.save_button);
        Button btnCancel = dialogView.findViewById(R.id.cancel_button);

        editName.setText(playlist.getName());
        editNote.setText(playlist.getNote());
        if (playlist.getCoverImage() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(playlist.getCoverImage(), 0, playlist.getCoverImage().length);
            coverPreview.setImageBitmap(bitmap);
            coverPreview.setVisibility(View.VISIBLE);
            selectedCoverImage = playlist.getCoverImage();
        } else {
            selectedCoverImage = null;
        }

        btnSelectCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newNote = editNote.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập tên playlist!", Toast.LENGTH_SHORT).show();
                return;
            }
            playlistDb.updatePlaylist(playlist.getId(), newName, newNote.isEmpty() ? null : newNote, selectedCoverImage);
            loadPlaylists();
            Toast.makeText(getContext(), "Đã cập nhật playlist '" + newName + "'!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            syncManager.syncPlaylistsToFirebase(); // Chỉ sync khi user sửa
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void deletePlaylist(Playlist playlist) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa playlist")
                .setMessage("Bạn có chắc chắn muốn xóa playlist '" + playlist.getName() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    playlistDb.deletePlaylist(playlist.getId());
                    loadPlaylists();
                    Toast.makeText(getContext(), "Đã xóa playlist '" + playlist.getName() + "'!", Toast.LENGTH_SHORT).show();
                    syncManager.syncPlaylistsToFirebase(); // Chỉ sync khi user xóa
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
    }

}

