package com.example.doanlmao;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Activity.PlayerActivity;
import com.example.doanlmao.Model.MusicViewModel;
import com.example.doanlmao.Model.Song;
import com.google.android.material.color.MaterialColors;

import java.io.ByteArrayOutputStream;

public class MiniPlayerController {
    private MainActivity activity;
    private LinearLayout miniPlayer;
    private ImageView miniPlayerArtwork, miniPlayerPlayPause, miniPlayerNext, miniPlayerPrev;
    private TextView miniPlayerTitle, miniPlayerArtist;
    private SeekBar miniPlayerSeekBar;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isControllable = false;
    private static final String TAG = "MiniPlayerController";
    private Bitmap processedArtwork;
    private boolean justScanned = false;
    private boolean isMiniPlayerVisible = false;
    private boolean isAnimating = false;
    private boolean isManuallyHidden = false; // Thêm để kiểm soát ẩn thủ công

    public MiniPlayerController(MainActivity activity) {
        this.activity = activity;
        miniPlayer = activity.findViewById(R.id.mini_player);
        miniPlayerTitle = activity.findViewById(R.id.mini_player_title);
        miniPlayerArtist = activity.findViewById(R.id.mini_player_artist);
        miniPlayerArtwork = activity.findViewById(R.id.mini_player_artwork);
        miniPlayerPlayPause = activity.findViewById(R.id.mini_player_play_pause);
        miniPlayerNext = activity.findViewById(R.id.mini_player_next);
        miniPlayerPrev = activity.findViewById(R.id.mini_player_prev);
        miniPlayerSeekBar = activity.findViewById(R.id.mini_player_seekbar);

        // Sửa cách áp dụng drawable
        LayerDrawable progressDrawable = (LayerDrawable) ContextCompat.getDrawable(activity, R.drawable.seekbar_material_you);
        if (progressDrawable != null) {
            Drawable progress = progressDrawable.findDrawableByLayerId(android.R.id.progress);
            if (progress != null) {
                // Áp dụng tint từ colorPrimary trong theme
                TypedValue outValue = new TypedValue();
                activity.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, outValue, true);
                progress.setTint(outValue.data);
            }
            miniPlayerSeekBar.setProgressDrawable(progressDrawable);
        }
        miniPlayerSeekBar.setThumb(null);

        miniPlayerPlayPause.setOnClickListener(v -> {
            if (isControllable && MainActivity.musicService != null) {
                if (MainActivity.musicService.isPlaying()) {
                    MainActivity.musicService.pauseSong();
                } else {
                    MainActivity.musicService.resumeSong();
                }
                justScanned = false;
            }
        });

        miniPlayerNext.setOnClickListener(v -> {
            if (isControllable && MainActivity.musicService != null) {
                MainActivity.musicService.playNextSong();
                justScanned = false;
            }
        });

        miniPlayerPrev.setOnClickListener(v -> {
            if (isControllable && MainActivity.musicService != null) {
                MainActivity.musicService.playPreviousSong();
                justScanned = false;
            }
        });

        miniPlayerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isControllable && MainActivity.musicService != null) {
                    int duration = MainActivity.musicService.getDuration();
                    int newPosition = (progress * duration) / 100;
                    MainActivity.musicService.mediaPlayer.seekTo(newPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        miniPlayer.setOnClickListener(v -> {
            if (isControllable) {
                Intent intent = new Intent(activity, PlayerActivity.class);
                activity.startActivity(intent);
            }
        });

        miniPlayer.setVisibility(View.GONE);
    }

    public void updateMiniPlayer() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MusicViewModel viewModel = new ViewModelProvider(activity).get(MusicViewModel.class);
                boolean isScanning = viewModel.isScanning().getValue() != null && viewModel.isScanning().getValue();

                if (activity.isServiceBound() && MainActivity.musicService != null && MainActivity.musicService.getSongList() != null && !MainActivity.musicService.getSongList().isEmpty()) {
                    int index = MainActivity.musicService.getCurrentSongIndex();
                    if (index >= 0 && index < MainActivity.musicService.getSongList().size()) {
                        Song song = MainActivity.musicService.getSongList().get(index);
                        miniPlayerTitle.setText(song.getTitle());
                        miniPlayerArtist.setText(song.getArtist());
                        if (song.getArtwork() != null) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(song.getArtwork(), 0, song.getArtwork().length);
                            miniPlayerArtwork.setImageBitmap(bitmap);
                            processedArtwork = bitmap;
                        } else {
                            miniPlayerArtwork.setImageResource(R.drawable.baseline_music_note_24);
                            processedArtwork = null;
                        }
                        miniPlayerPlayPause.setImageResource(MainActivity.musicService.isPlaying() ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
                        int duration = MainActivity.musicService.getDuration();
                        int currentPosition = MainActivity.musicService.getCurrentPosition();
                        if (duration > 0) {
                            miniPlayerSeekBar.setMax(100);
                            miniPlayerSeekBar.setProgress((int) ((currentPosition * 100L) / duration));
                            Log.d(TAG, "SeekBar Progress: " + miniPlayerSeekBar.getProgress() + ", Current: " + currentPosition + ", Duration: " + duration);
                        }

                        if (justScanned || isScanning || isManuallyHidden) {
                            setMiniPlayerVisible(false);
                        } else {
                            setMiniPlayerVisible(true);
                        }
                    } else {
                        setMiniPlayerVisible(false);
                        Log.w(TAG, "Invalid song index: " + index);
                    }
                } else {
                    setMiniPlayerVisible(false);
                    Log.w(TAG, "MusicService not bound or song list empty");
                }
                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    public void setManuallyHidden(boolean hidden) {
        isManuallyHidden = hidden;
        setMiniPlayerVisible(!hidden);
    }
    public void setControllable(boolean controllable) {
        isControllable = controllable;
        miniPlayerPlayPause.setEnabled(isControllable);
        miniPlayerNext.setEnabled(isControllable);
        miniPlayerPrev.setEnabled(isControllable);
        miniPlayerSeekBar.setEnabled(isControllable);
        Log.d(TAG, "MiniPlayer controllable set to: " + isControllable);
    }

    public void cleanup() {
        handler.removeCallbacksAndMessages(null);
        processedArtwork = null;
    }

    public void setJustScanned(boolean scanned) {
        justScanned = scanned;
    }

    public void setMiniPlayerVisible(boolean visible) {
        if (MainActivity.musicService == null || MainActivity.musicService.getSongList() == null || MainActivity.musicService.getSongList().isEmpty()) {
            miniPlayer.setVisibility(View.GONE);
            isMiniPlayerVisible = false;
            return;
        }

        if (isAnimating || isMiniPlayerVisible == visible) return;

        isMiniPlayerVisible = visible;
        isAnimating = true;

        if (visible) {
            miniPlayer.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(miniPlayer, "translationY", miniPlayer.getHeight(), 0);
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    isAnimating = false;
                }
            });
            animator.start();
        } else {
            ObjectAnimator animator = ObjectAnimator.ofFloat(miniPlayer, "translationY", 0, miniPlayer.getHeight());
            animator.setDuration(200);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    miniPlayer.setVisibility(View.GONE);
                    miniPlayer.setTranslationY(0);
                    isAnimating = false;
                }
            });
            animator.start();
        }
        Log.d(TAG, "MiniPlayer visibility set to: " + visible);
    }

    public boolean isMiniPlayerVisible() {
        return isMiniPlayerVisible;
    }
}