package com.example.doanlmao.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.doanlmao.Model.Playlist;
import com.example.doanlmao.R;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    private final Context context;
    private final List<Playlist> playlistList;
    private final OnPlaylistClickListener clickListener;
    private final OnPlaylistLongClickListener longClickListener;

    public PlaylistAdapter(Context context, List<Playlist> playlistList,
                           OnPlaylistClickListener clickListener,
                           OnPlaylistLongClickListener longClickListener) {
        this.context = context;
        this.playlistList = playlistList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @Override
    public PlaylistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.playlist_item, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PlaylistViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);
        holder.playlistName.setText(playlist.getName());
        holder.playlistNote.setText(playlist.getNote() != null ? playlist.getNote() : "Không có ghi chú");

        if (playlist.getCoverImage() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(playlist.getCoverImage(), 0, playlist.getCoverImage().length);
            holder.playlistImage.setImageBitmap(bitmap);
        } else {
            holder.playlistImage.setImageResource(R.drawable.baseline_music_note_24);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onPlaylistClick(playlist));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onPlaylistLongClick(playlist);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    // Thêm hàm này để cập nhật danh sách
    public void updatePlaylists(List<Playlist> newPlaylistList) {
        playlistList.clear();
        playlistList.addAll(newPlaylistList);
        notifyDataSetChanged();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        ImageView playlistImage;
        TextView playlistName;
        TextView playlistNote;

        PlaylistViewHolder(View itemView) {
            super(itemView);
            playlistImage = itemView.findViewById(R.id.playlist_image);
            playlistName = itemView.findViewById(R.id.playlist_name);
            playlistNote = itemView.findViewById(R.id.playlist_note);
        }
    }

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public interface OnPlaylistLongClickListener {
        void onPlaylistLongClick(Playlist playlist);
    }
}