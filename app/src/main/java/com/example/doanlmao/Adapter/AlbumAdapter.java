package com.example.doanlmao.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanlmao.Model.Album;
import com.example.doanlmao.R;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private Context context;
    private List<Album> albumList;
    private OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album, int position);
    }

    public AlbumAdapter(Context context, List<Album> albumList, OnAlbumClickListener listener) {
        this.context = context;
        this.albumList = albumList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.album_item, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        if (albumList == null || position >= albumList.size()) {
            Log.e("AlbumAdapter", "Invalid position or null albumList: " + position);
            return;
        }
        Album album = albumList.get(position);
        holder.albumName.setText(album.getName());
        holder.artistName.setText(album.getArtist());
        if (album.getArtwork() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(album.getArtwork(), 0, album.getArtwork().length);
            holder.albumImage.setImageBitmap(bitmap);
            Log.d("AlbumAdapter", "Bound album: " + album.getName() + " with artwork");
        } else {
            holder.albumImage.setImageResource(R.drawable.baseline_music_note_24);
            Log.d("AlbumAdapter", "Bound album: " + album.getName() + " without artwork");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlbumClick(album, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return albumList != null ? albumList.size() : 0; // Thêm kiểm tra null
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView albumImage;
        TextView albumName, artistName;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            albumImage = itemView.findViewById(R.id.album_image);
            albumName = itemView.findViewById(R.id.album_name);
            artistName = itemView.findViewById(R.id.artist_name);
        }
    }
}
