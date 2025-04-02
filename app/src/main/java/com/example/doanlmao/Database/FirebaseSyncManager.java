package com.example.doanlmao.Database;

import com.example.doanlmao.Model.Playlist;
import com.example.doanlmao.Model.Song;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSyncManager {
    private static final String TAG = "FirebaseSyncManager";
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private PlaylistDatabase playlistDb;

    public FirebaseSyncManager(PlaylistDatabase playlistDb) {
        //Thay bằng của bạn nếu muốn sử dụng riêng
        this.mAuth = FirebaseAuth.getInstance();
        this.playlistDb = playlistDb;
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // deRef = FirebaseDatabase.getInstance("https://your-project-id-default-rtdb.firebaseio.com"); // Thay bằng database URL của bạn
            dbRef = FirebaseDatabase.getInstance()// Để vậy nếu bạn tự thêm google-services.json
                    .getReference("playlists")
                    .child(user.getUid());
        } else {
            Log.w(TAG, "User is null during initialization");
        }
    }

    public void syncPlaylistsToFirebase() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || dbRef == null) {
            Log.w(TAG, "User not logged in, skipping sync");
            return;
        }

        List<Playlist> playlists = playlistDb.getAllPlaylists();
        Map<String, Object> playlistsMap = new HashMap<>();

        for (Playlist playlist : playlists) {
            Map<String, Object> playlistData = new HashMap<>();
            playlistData.put("name", playlist.getName());
            playlistData.put("note", playlist.getNote());
            if (playlist.getCoverImage() != null) {
                String coverBase64 = Base64.encodeToString(playlist.getCoverImage(), Base64.DEFAULT);
                playlistData.put("coverImage", coverBase64);
            }
            List<String> songs = playlistDb.getSongsInPlaylist(playlist.getId());
            playlistData.put("songs", songs);

            playlistsMap.put(String.valueOf(playlist.getId()), playlistData);
        }

        dbRef.setValue(playlistsMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Playlists synced to Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Sync failed: " + e.getMessage()));
    }

    public void fetchPlaylistsFromFirebase(OnSyncCompleteListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || dbRef == null) {
            Log.w(TAG, "User not logged in, skipping fetch");
            if (listener != null) listener.onSyncComplete();
            return;
        }

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "No data to fetch from Firebase");
                    if (listener != null) listener.onSyncComplete();
                    return;
                }

                for (DataSnapshot playlistSnap : snapshot.getChildren()) {
                    long playlistId;
                    try {
                        playlistId = Long.parseLong(playlistSnap.getKey());
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid playlist ID: " + playlistSnap.getKey());
                        continue;
                    }

                    String name = playlistSnap.child("name").getValue(String.class);
                    String note = playlistSnap.child("note").getValue(String.class);
                    String coverBase64 = playlistSnap.child("coverImage").getValue(String.class);
                    byte[] coverImage = coverBase64 != null ? Base64.decode(coverBase64, Base64.DEFAULT) : null;

                    List<Playlist> localPlaylists = playlistDb.getAllPlaylists();
                    boolean exists = false;
                    for (Playlist p : localPlaylists) {
                        if (p.getId() == playlistId) {
                            exists = true;
                            playlistDb.updatePlaylist(playlistId, name, note, coverImage);
                            break;
                        }
                    }
                    if (!exists) {
                        // Thêm playlist với ID từ Firebase
                        playlistDb.addPlaylistWithId(playlistId, name, note, coverImage);
                    }

                    // Sync songs
                    DataSnapshot songsSnap = playlistSnap.child("songs");
                    List<String> firebaseSongs = new ArrayList<>();
                    for (DataSnapshot songSnap : songsSnap.getChildren()) {
                        String songPath = songSnap.getValue(String.class);
                        if (songPath != null) firebaseSongs.add(songPath);
                    }

                    List<String> currentSongs = playlistDb.getSongsInPlaylist(playlistId);
                    for (String songPath : currentSongs) {
                        if (!firebaseSongs.contains(songPath)) {
                            playlistDb.removeSongFromPlaylist(playlistId, songPath);
                        }
                    }
                    for (String songPath : firebaseSongs) {
                        if (!currentSongs.contains(songPath)) {
                            playlistDb.addSongToPlaylist(playlistId, songPath);
                        }
                    }
                }
                Log.d(TAG, "Playlists fetched from Firebase");
                if (listener != null) listener.onSyncComplete();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Fetch failed: " + error.getMessage());
                if (listener != null) listener.onSyncComplete();
            }
        });
    }

    public void listenForPlaylistChanges(OnPlaylistUpdatedListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || dbRef == null || playlistDb == null) {
            Log.w(TAG, "Cannot listen: user=" + user + ", dbRef=" + dbRef + ", playlistDb=" + playlistDb);
            return;
        }

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "Data changed: " + snapshot.toString());
                if (!snapshot.exists()) {
                    Log.d(TAG, "No playlists on Firebase, clearing local");
                    for (Playlist p : playlistDb.getAllPlaylists()) {
                        playlistDb.deletePlaylist(p.getId());
                    }
                    if (listener != null) listener.onPlaylistUpdated();
                    return;
                }

                List<Long> firebasePlaylistIds = new ArrayList<>();

                for (DataSnapshot playlistSnap : snapshot.getChildren()) {
                    long playlistId;
                    try {
                        playlistId = Long.parseLong(playlistSnap.getKey());
                        firebasePlaylistIds.add(playlistId);
                        Log.d(TAG, "Processing playlist ID: " + playlistId);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid playlist ID: " + playlistSnap.getKey(), e);
                        continue;
                    }

                    String name = playlistSnap.child("name").getValue(String.class);
                    String note = playlistSnap.child("note").getValue(String.class);
                    String coverBase64 = playlistSnap.child("coverImage").getValue(String.class);
                    byte[] coverImage = null;
                    if (coverBase64 != null) {
                        try {
                            coverImage = Base64.decode(coverBase64, Base64.DEFAULT);
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "Failed to decode coverImage for playlist " + playlistId, e);
                        }
                    }

                    List<Playlist> localPlaylists = playlistDb.getAllPlaylists();
                    boolean exists = false;
                    for (Playlist p : localPlaylists) {
                        if (p.getId() == playlistId) {
                            exists = true;
                            playlistDb.updatePlaylist(playlistId, name, note, coverImage);
                            Log.d(TAG, "Updated existing playlist: " + playlistId);
                            break;
                        }
                    }
                    if (!exists) {
                        // Thêm playlist với ID từ Firebase
                        playlistDb.addPlaylistWithId(playlistId, name, note, coverImage);
                        Log.d(TAG, "Added new playlist with ID: " + playlistId);
                    }

                    List<String> currentSongs = playlistDb.getSongsInPlaylist(playlistId);
                    DataSnapshot songsSnap = playlistSnap.child("songs");
                    List<String> firebaseSongs = new ArrayList<>();
                    for (DataSnapshot songSnap : songsSnap.getChildren()) {
                        String songPath = songSnap.getValue(String.class);
                        if (songPath != null) firebaseSongs.add(songPath);
                    }

                    for (String songPath : currentSongs) {
                        if (!firebaseSongs.contains(songPath)) {
                            playlistDb.removeSongFromPlaylist(playlistId, songPath);
                            Log.d(TAG, "Removed song " + songPath + " from playlist " + playlistId);
                        }
                    }
                    for (String songPath : firebaseSongs) {
                        if (!currentSongs.contains(songPath)) {
                            playlistDb.addSongToPlaylist(playlistId, songPath);
                            Log.d(TAG, "Added song " + songPath + " to playlist " + playlistId);
                        }
                    }
                }

                List<Playlist> localPlaylists = playlistDb.getAllPlaylists();
                for (Playlist p : localPlaylists) {
                    if (!firebasePlaylistIds.contains(p.getId())) {
                        playlistDb.deletePlaylist(p.getId());
                        Log.d(TAG, "Deleted local playlist not in Firebase: " + p.getId());
                    }
                }

                Log.d(TAG, "Playlists updated from Firebase");
                if (listener != null) {
                    listener.onPlaylistUpdated();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Real-time sync cancelled: " + error.getMessage());
            }
        });
    }

    public interface OnSyncCompleteListener {
        void onSyncComplete();
    }

    public interface OnPlaylistUpdatedListener {
        void onPlaylistUpdated();
    }
}

