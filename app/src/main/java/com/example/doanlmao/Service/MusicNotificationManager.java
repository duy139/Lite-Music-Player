package com.example.doanlmao.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.doanlmao.Activity.PlayerActivity;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;

public class MusicNotificationManager {
    private static final String CHANNEL_ID = "music_channel";
    private static final String CHANNEL_NAME = "Music Player";
    private static final int NOTIFICATION_ID = 1;

    private final Context context;
    private final NotificationManager notificationManager;

    public MusicNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Music playback controls");
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showNotification(Song song, boolean isPlaying, int currentIndex) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("currentIndex", currentIndex);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent pauseIntent = new Intent(context, MusicService.class).setAction("PAUSE");
        PendingIntent pausePendingIntent = PendingIntent.getService(
                context, 0, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent playIntent = new Intent(context, MusicService.class).setAction("PLAY");
        PendingIntent playPendingIntent = PendingIntent.getService(
                context, 0, playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent nextIntent = new Intent(context, MusicService.class).setAction("NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(
                context, 0, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent prevIntent = new Intent(context, MusicService.class).setAction("PREV");
        PendingIntent prevPendingIntent = PendingIntent.getService(
                context, 0, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Bitmap artworkBitmap = null;
        if (song != null && song.getArtwork() != null) {
            artworkBitmap = BitmapFactory.decodeByteArray(song.getArtwork(), 0, song.getArtwork().length);
        }

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_music_note_24)
                .setContentTitle(song != null ? song.getTitle() : "Unknown")
                .setContentText(song != null ? song.getArtist() : "Unknown")
                .setContentIntent(pendingIntent)
                .setLargeIcon(artworkBitmap)
                .addAction(R.drawable.baseline_skip_previous_24, "Prev", prevPendingIntent)
                .addAction(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24, isPlaying ? "Pause" : "Play", isPlaying ? pausePendingIntent : playPendingIntent)
                .addAction(R.drawable.baseline_skip_next_24, "Next", nextPendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(null)
                .build();

        ((MusicService) context).startForeground(NOTIFICATION_ID, notification);
    }

    public void hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID); // Xóa notification
        ((MusicService) context).stopForeground(true); // Dừng foreground service
        Log.d("MusicNotificationManager", "Notification hidden and foreground stopped");
    }
}