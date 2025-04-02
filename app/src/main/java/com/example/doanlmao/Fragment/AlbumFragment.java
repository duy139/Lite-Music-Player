package com.example.doanlmao.Fragment;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Adapter.AlbumAdapter;
import com.example.doanlmao.Activity.AlbumDetailsActivity;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Misc.SongSorter;
import com.example.doanlmao.Model.Album;
import com.example.doanlmao.Model.MusicViewModel;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumFragment extends Fragment {
    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private static final String TAG = "AlbumFragment";
    private MusicViewModel viewModel;
    private SongSorter songSorter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);

        // Kiểm tra orientation và set số cột
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), isLandscape ? 4 : 2));
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

        if (getContext() == null) {
            Log.e(TAG, "Context is null in onCreateView");
            return view;
        }

        // Khởi tạo SongSorter
        songSorter = new SongSorter(getContext());

        viewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);
        initializeAdapter();

        viewModel.getAlbums().observe(getViewLifecycleOwner(), albums -> {
            albumAdapter = new AlbumAdapter(getContext(), albums, new AlbumAdapter.OnAlbumClickListener() {
                @Override
                public void onAlbumClick(Album album, int position) {
                    if (getContext() != null) {
                        Intent intent = new Intent(getContext(), AlbumDetailsActivity.class);
                        intent.putExtra("albumName", album.getName());
                        intent.putExtra("artistName", album.getArtist());
                        intent.putExtra("artwork", album.getArtwork());
                        startActivity(intent);
                        Log.d(TAG, "Navigating to AlbumDetails for album: " + album.getName());
                    }
                }
            });
            recyclerView.setAdapter(albumAdapter);
            // Áp dụng sắp xếp cuối cùng khi dữ liệu thay đổi
            applyLastSort(albums);
            Log.d(TAG, "Loaded " + albums.size() + " albums from ViewModel");
        });

        setHasOptionsMenu(true); // Cho phép Fragment xử lý menu

        return view;
    }

    private void initializeAdapter() {
        albumAdapter = new AlbumAdapter(getContext(), new ArrayList<>(), null);
        recyclerView.setAdapter(albumAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu_album, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_more) {
            showSortDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyLastSort(List<Album> albums) {
        int lastSortType = songSorter.getLastSortType();
        boolean lastSortAscending = songSorter.getLastSortAscending();
        songSorter.sortAlbums(albums, lastSortType, lastSortAscending);
        if (albumAdapter != null) {
            albumAdapter.notifyDataSetChanged();
        }
    }

    private void showSortDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sort_options_album, null);
        dialog.setContentView(dialogView);

        RadioGroup sortTitleGroup = dialogView.findViewById(R.id.sort_title_group);
        RadioButton sortTitleAz = dialogView.findViewById(R.id.sort_title_az);
        RadioButton sortTitleZa = dialogView.findViewById(R.id.sort_title_za);

        RadioGroup sortArtistGroup = dialogView.findViewById(R.id.sort_artist_group);
        RadioButton sortArtistAz = dialogView.findViewById(R.id.sort_artist_az);
        RadioButton sortArtistZa = dialogView.findViewById(R.id.sort_artist_za);

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
        }

        sortTitleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_title_az) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_TITLE, true);
            } else if (checkedId == R.id.sort_title_za) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_TITLE, false);
            }
            applyLastSort(viewModel.getAlbums().getValue());
            dialog.dismiss();
        });

        sortArtistGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_artist_az) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_ARTIST, true);
            } else if (checkedId == R.id.sort_artist_za) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_ARTIST, false);
            }
            applyLastSort(viewModel.getAlbums().getValue());
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            recyclerView.scrollToPosition(0);
            Log.d(TAG, "Refreshed albums in onResume");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
        albumAdapter = null;
        Log.d(TAG, "onDestroyView called");
    }
}