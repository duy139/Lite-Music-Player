package com.example.doanlmao.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanlmao.Activity.PlayerActivity;
import com.example.doanlmao.Database.FavoriteDatabase;
import com.example.doanlmao.Fragment.FavoriteFragment;
import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.google.android.material.color.MaterialColors;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Song> songList;
    public static List<Song> globalSongList;
    private FavoriteDatabase favoriteDb;
    private static final long DEBOUNCE_DELAY = 500;
    private long lastClickTime = 0;
    private OnSongLongClickListener longClickListener;
    private String title;
    private String artistName;
    private String note;
    private byte[] artwork;
    private OnBackClickListener backClickListener;
    private OnAddClickListener addClickListener; // Listener mới cho nút "Thêm"
    private boolean showHeader;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SONG = 1;

    public interface OnSongLongClickListener {
        void onSongLongClick(int position);
    }

    public interface OnBackClickListener {
        void onBackClick();
    }

    public interface OnAddClickListener {
        void onAddClick();
    }

    public SongAdapter(Context context, List<Song> musicList, String albumName, String artistName, byte[] artwork) {
        this.context = context;
        this.songList = musicList;
        this.title = albumName;
        this.artistName = artistName;
        this.artwork = artwork;
        this.favoriteDb = new FavoriteDatabase(context);
        globalSongList = musicList;
        this.showHeader = true;
        this.note = null;
    }

    public SongAdapter(Context context, List<Song> musicList, String playlistName, byte[] artwork, String note) {
        this.context = context;
        this.songList = musicList;
        this.title = playlistName;
        this.artwork = artwork;
        this.favoriteDb = new FavoriteDatabase(context);
        globalSongList = musicList;
        this.showHeader = true;
        this.artistName = null;
        this.note = note;
    }

    public SongAdapter(Context context, List<Song> musicList) {
        this.context = context;
        this.songList = musicList;
        this.favoriteDb = new FavoriteDatabase(context);
        globalSongList = musicList;
        this.showHeader = false;
        this.title = null;
        this.artistName = null;
        this.artwork = null;
        this.note = null;
    }

    public void setOnSongLongClickListener(OnSongLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnBackClickListener(OnBackClickListener listener) {
        this.backClickListener = listener;
    }

    public void setOnAddClickListener(OnAddClickListener listener) {
        this.addClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (showHeader && position == 0) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_SONG;
    }

    @Override
    public int getItemCount() {
        int count = songList != null ? songList.size() : 0;
        return showHeader ? count + 1 : count;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(
                    artistName != null ? R.layout.header_album_details : R.layout.header_playlist_details,
                    parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
            return new MusicViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

            int onSurfaceColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
            if (artistName != null) { // Album
                headerHolder.albName.setTextColor(onSurfaceColor);
                headerHolder.artName.setTextColor(onSurfaceColor);
            } else { // Playlist
                headerHolder.playlistName.setTextColor(onSurfaceColor);
                headerHolder.noteText.setTextColor(onSurfaceColor);
                headerHolder.addButton.setColorFilter(onSurfaceColor);
            }
            headerHolder.backButton.setColorFilter(onSurfaceColor);

            if (artistName != null) { // Album
                headerHolder.albName.setText(title != null && !title.trim().isEmpty() ? title : "Unknown Album");
                headerHolder.artName.setText(artistName != null && !artistName.trim().isEmpty() ? artistName : "Unknown Artist");
            } else { // Playlist
                headerHolder.playlistName.setText(title != null && !title.trim().isEmpty() ? title : "Unknown Playlist");
                headerHolder.noteText.setText(note != null && !note.trim().isEmpty() ? note : "Không có ghi chú");
                headerHolder.addButton.setOnClickListener(v -> {
                    if (addClickListener != null) {
                        addClickListener.onAddClick();
                    }
                });
            }

            if (artwork != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork.length);
                headerHolder.photo.setImageBitmap(bitmap);
            } else {
                headerHolder.photo.setImageResource(R.drawable.baseline_music_note_24);
            }

            headerHolder.backButton.setOnClickListener(v -> {
                if (backClickListener != null) {
                    backClickListener.onBackClick();
                }
            });
        } else {
            int songPosition = showHeader ? position - 1 : position;
            Song song = songList.get(songPosition);
            MusicViewHolder musicHolder = (MusicViewHolder) holder;

            int onSurfaceColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
            musicHolder.songTitle.setTextColor(onSurfaceColor);
            musicHolder.artistName.setTextColor(onSurfaceColor);
            musicHolder.duration.setTextColor(onSurfaceColor);
            musicHolder.favoriteIcon.setColorFilter(onSurfaceColor);

            musicHolder.songTitle.setText(song.getTitle());

            String artist = song.getArtist();
            if (artist == null || artist.trim().isEmpty()) {
                artist = "Unknown Artist";
            }
            String album = song.getAlbum();
            if (album == null || album.trim().isEmpty()) {
                album = "Unknown Album";
            }
            musicHolder.artistName.setText(artist + " - " + album);

            musicHolder.duration.setText(song.getDuration());

            if (song.getArtwork() != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(song.getArtwork(), 0, song.getArtwork().length);
                musicHolder.musicImg.setImageBitmap(bitmap);
            } else {
                musicHolder.musicImg.setImageResource(R.drawable.baseline_music_note_24);
            }

            boolean isFavorite = favoriteDb.isFavorite(song.getPath());
            musicHolder.favoriteIcon.setImageResource(isFavorite ? R.drawable.baseline_favorite_24 : R.drawable.baseline_favorite_border_24);

            musicHolder.favoriteIcon.setOnClickListener(v -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < DEBOUNCE_DELAY) return;
                lastClickTime = currentTime;

                new Thread(() -> {
                    boolean isFavoriteBefore = favoriteDb.isFavorite(song.getPath());
                    if (isFavoriteBefore) {
                        favoriteDb.removeFavorite(song.getPath());
                        updateFavoriteUI(musicHolder, false, "Đã xóa khỏi yêu thích");
                    } else {
                        favoriteDb.addFavorite(song);
                        updateFavoriteUI(musicHolder, true, "Đã thêm vào yêu thích");
                    }
                    if (context instanceof MainActivity) {
                        Fragment fragment = ((MainActivity) context).getSupportFragmentManager().findFragmentById(R.id.frameLayout);
                        if (fragment instanceof FavoriteFragment) {
                            ((MainActivity) context).runOnUiThread(() -> ((FavoriteFragment) fragment).refreshFavorites());
                        }
                    }
                }).start();
            });

            musicHolder.itemView.setOnClickListener(v -> {
                if (MainActivity.musicService != null) {
                    int originalPosition = globalSongList.indexOf(songList.get(songPosition));
                    MainActivity.musicService.setSongList(globalSongList, originalPosition);
                    MainActivity.musicService.playSong();

                    // Hiện mini player ngay khi chọn bài hát mới
                    if (context instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) context;
                        if (mainActivity.miniPlayerController != null) {
                            mainActivity.miniPlayerController.setMiniPlayerVisible(true);
                        }
                    }
                }
                Intent intent = new Intent(context, PlayerActivity.class);
                int currentIndex = globalSongList.indexOf(songList.get(songPosition));
                intent.putExtra("currentIndex", currentIndex);
                intent.putExtra("currentPosition", 0);
                intent.putExtra("isPlaying", true);
                context.startActivity(intent);
            });

            musicHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onSongLongClick(songPosition);
                    return true;
                }
                return false;
            });
        }
    }

    private void updateFavoriteUI(MusicViewHolder holder, boolean isFavorite, String toastMessage) {
        new Handler(Looper.getMainLooper()).post(() -> {
            holder.favoriteIcon.setImageResource(isFavorite ? R.drawable.baseline_favorite_24 : R.drawable.baseline_favorite_border_24);
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
        });
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView albName, artName; // Cho album
        TextView playlistName, noteText; // Cho playlist
        ImageButton backButton;
        ImageButton addButton; // Nút "Thêm" cho playlist

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            backButton = itemView.findViewById(R.id.backButton);
            photo = itemView.findViewById(R.id.albumPhoto); // Mặc định cho album
            if (itemView.findViewById(R.id.alb_name) != null) { // Album
                albName = itemView.findViewById(R.id.alb_name);
                artName = itemView.findViewById(R.id.art_name);
            } else { // Playlist
                photo = itemView.findViewById(R.id.playlistPhoto);
                playlistName = itemView.findViewById(R.id.playlist_name);
                noteText = itemView.findViewById(R.id.note_text);
                addButton = itemView.findViewById(R.id.addButton);
            }
        }
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView songTitle, artistName, duration;
        ImageView musicImg, favoriteIcon;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.music_file_name);
            artistName = itemView.findViewById(R.id.music_artist_name);
            duration = itemView.findViewById(R.id.duration);
            musicImg = itemView.findViewById(R.id.music_img);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
        }
    }
}
