package com.example.doanlmao.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Database.FavoriteDatabase;
import com.example.doanlmao.Misc.SongSorter;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private List<Song> favoriteList;
    private FavoriteDatabase favoriteDb;
    private static final String TAG = "FavoriteFragment";
    private SongSorter songSorter; // Thêm SongSorter

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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

        favoriteDb = new FavoriteDatabase(requireContext());
        favoriteList = new ArrayList<>();
        songSorter = new SongSorter(requireContext()); // Khởi tạo SongSorter

        loadFavorites();

        songAdapter = new SongAdapter(requireContext(), favoriteList);
        recyclerView.setAdapter(songAdapter);

        setHasOptionsMenu(true); // Cho phép Fragment xử lý menu

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu_favorite, menu);
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

    private void loadFavorites() {
        favoriteList.clear();
        favoriteList.addAll(favoriteDb.getAllFavorites());
        // Áp dụng sắp xếp cuối cùng từ SongSorter
        int lastSortType = songSorter.getLastSortType();
        boolean lastSortAscending = songSorter.getLastSortAscending();
        songSorter.sortSongs(favoriteList, lastSortType, lastSortAscending);
        if (songAdapter != null) {
            songAdapter.notifyDataSetChanged();
        }
        Log.d(TAG, "Loaded " + favoriteList.size() + " favorite songs");
    }

    public void refreshFavorites() {
        loadFavorites();
    }

    private void showSortDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sort_options, null);
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
            loadFavorites();
            dialog.dismiss();
        });

        sortArtistGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_artist_az) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_ARTIST, true);
            } else if (checkedId == R.id.sort_artist_za) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_ARTIST, false);
            }
            loadFavorites();
            dialog.dismiss();
        });

        sortYearGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_year_asc) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_YEAR, true);
            } else if (checkedId == R.id.sort_year_desc) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_YEAR, false);
            }
            loadFavorites();
            dialog.dismiss();
        });

        sortDurationGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sort_duration_asc) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_DURATION, true);
            } else if (checkedId == R.id.sort_duration_desc) {
                songSorter.saveSortPreference(SongSorter.SORT_BY_DURATION, false);
            }
            loadFavorites();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean-up nếu cần, nhưng không cần close database vì SQLiteOpenHelper tự quản lý
        // favoriteDb.close(); // Không cần gọi close, để SQLiteOpenHelper tự xử lý
    }
}