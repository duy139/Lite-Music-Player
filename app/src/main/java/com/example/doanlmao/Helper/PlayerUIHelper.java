package com.example.doanlmao.Helper;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.documentfile.provider.DocumentFile;
import androidx.palette.graphics.Palette;

import com.example.doanlmao.Misc.ColorExtractor;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.example.doanlmao.Service.MusicService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class PlayerUIHelper {
    private final Activity activity;
    private static final String TAG = "PlayerUIHelper";

    // UI elements
    private View mContainer;
    private ImageView songImageView, shuffleButton, prevButton, nextButton, repeatButton;
    private ImageView favoriteButton, speedButton, timerButton;
    private FloatingActionButton playPauseButton;
    private TextView songNameTextView, songArtistTextView, songAlbumTextView, durationPlayed, durationTotal;
    private SeekBar seekBar;
    private Bitmap currentArtwork;
    private int dominantColor = Color.WHITE;
    private ColorExtractor colorExtractor;

    public PlayerUIHelper(Activity activity) {
        this.activity = activity;
        initializeViews();

        colorExtractor = new ColorExtractor(activity, mContainer, (newDominantColor, newTextColor) -> {
            this.dominantColor = newDominantColor;
            updateButtonColorsWithColor(newDominantColor);
            updateTextColorsWithColor(newTextColor);
        });
    }

    private void setupMarquee(TextView textView) {
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setSingleLine(true);
        textView.setMarqueeRepeatLimit(-1);

        // Dùng ViewTreeObserver để đo sau khi layout hoàn tất
        ViewTreeObserver vto = textView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Xóa listener để không gọi lại
                textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                float textWidth = textView.getPaint().measureText(textView.getText().toString());
                float viewWidth = textView.getWidth();

                Log.d(TAG, "Text: " + textView.getText() + ", textWidth: " + textWidth + ", viewWidth: " + viewWidth);

                float threshold = viewWidth * 1.2f;
                if (textWidth > threshold && viewWidth > 0) {
                    textView.setSelected(true);
                    textView.requestFocus();
                } else {
                    textView.setSelected(false);
                    textView.setGravity(Gravity.CENTER_HORIZONTAL);
                }
            }
        });
    }

    private void initializeViews() {
        mContainer = activity.findViewById(R.id.mContainer);
        songImageView = activity.findViewById(R.id.cover_art);
        shuffleButton = activity.findViewById(R.id.id_shuffle);
        prevButton = activity.findViewById(R.id.id_prev);
        nextButton = activity.findViewById(R.id.id_next);
        repeatButton = activity.findViewById(R.id.id_repeat);
        favoriteButton = activity.findViewById(R.id.favorite_button);
        speedButton = activity.findViewById(R.id.speed_button);
        timerButton = activity.findViewById(R.id.timer_button);
        playPauseButton = activity.findViewById(R.id.play_pause);
        songNameTextView = activity.findViewById(R.id.song_name);
        songArtistTextView = activity.findViewById(R.id.song_artist);
        songAlbumTextView = activity.findViewById(R.id.song_album);
        durationPlayed = activity.findViewById(R.id.durationPlayed);
        durationTotal = activity.findViewById(R.id.durationTotal);
        seekBar = activity.findViewById(R.id.seekBar);

        if (seekBar != null) {
            seekBar.setThumb(null);
        }

        songNameTextView.post(() -> setupMarquee(songNameTextView));
        songArtistTextView.post(() -> setupMarquee(songArtistTextView));
        songAlbumTextView.post(() -> setupMarquee(songAlbumTextView));
    }

    public void updateSongUI(Song song) {
        if (song != null) {
            songNameTextView.setText(song.getTitle());
            songArtistTextView.setText(song.getArtist());
            songAlbumTextView.setText(song.getAlbum());
            songImageView.setImageResource(R.drawable.baseline_music_note_24);

            songNameTextView.setSelected(false);
            songArtistTextView.setSelected(false);
            songAlbumTextView.setSelected(false);
            songNameTextView.post(() -> setupMarquee(songNameTextView));
            songArtistTextView.post(() -> setupMarquee(songArtistTextView));
            songAlbumTextView.post(() -> setupMarquee(songAlbumTextView));

            if (song.getArtwork() != null) {
                new Thread(() -> {
                    Bitmap artwork = BitmapFactory.decodeByteArray(song.getArtwork(), 0, song.getArtwork().length);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        songImageView.setImageBitmap(artwork);
                        colorExtractor.extractColors(artwork);
                    });
                }).start();
            } else {
                colorExtractor.extractColors(null);
            }
        }
    }



    private void updateButtonColorsWithColor(int color) {
        shuffleButton.setColorFilter(color);
        prevButton.setColorFilter(color);
        nextButton.setColorFilter(color);
        repeatButton.setColorFilter(color);
        playPauseButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        playPauseButton.setColorFilter(getContrastColor(color));
        favoriteButton.setColorFilter(color);
        speedButton.setColorFilter(color);
        timerButton.setColorFilter(color);

        if (seekBar != null) {
            Drawable progressDrawable = seekBar.getProgressDrawable();
            if (progressDrawable != null) {
                progressDrawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                Log.w(TAG, "SeekBar progressDrawable is null");
            }
            seekBar.setThumb(null);
        } else {
            Log.w(TAG, "SeekBar is null");
        }
    }

    private void updateTextColorsWithColor(int color) {
        songNameTextView.setTextColor(color);
        songArtistTextView.setTextColor(color);
        songAlbumTextView.setTextColor(color);
        durationPlayed.setTextColor(color);
        durationTotal.setTextColor(color);
    }

    public void updateAllUIStates(boolean isPlaying, boolean isShuffling, boolean isRepeating, boolean isFavorite) {
        updatePlayPauseIcon(isPlaying);
        updateShuffleIcon(isShuffling);
        updateRepeatIcon(isRepeating);
        updateFavoriteIcon(isFavorite);
    }

    public void updatePlayPauseIcon(boolean isPlaying) {
        playPauseButton.setImageResource(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.baseline_play_arrow_24);
        playPauseButton.setColorFilter(getContrastColor(dominantColor));
    }

    public void updateShuffleIcon(boolean isShuffling) {
        shuffleButton.setImageResource(isShuffling ? R.drawable.baseline_shuffle_on_24 : R.drawable.baseline_shuffle_24);
        shuffleButton.setColorFilter(dominantColor);
    }

    public void updateRepeatIcon(boolean isRepeating) {
        repeatButton.setImageResource(isRepeating ? R.drawable.baseline_repeat_on_24 : R.drawable.baseline_repeat_24);
        repeatButton.setColorFilter(dominantColor);
    }

    public void updateFavoriteIcon(boolean isFavorite) {
        favoriteButton.setImageResource(isFavorite ? R.drawable.baseline_favorite_24 : R.drawable.baseline_favorite_border_24);
        favoriteButton.setColorFilter(dominantColor);
    }

    public void updateSeekBar(int currentPosition, int duration) {
        seekBar.setMax(duration);
        seekBar.setProgress(currentPosition);
        durationPlayed.setText(formatDuration(currentPosition));
        durationTotal.setText(formatDuration(duration));
    }

    public String formatDuration(int durationMs) {
        int seconds = durationMs / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public void showSpeedDialog(MusicService musicService, Runnable onSpeedChanged) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_speed, null);
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView);

        AlertDialog dialog = dialogBuilder.create();

        TextView speedTitle = dialogView.findViewById(R.id.speed_title);
        SeekBar speedSeekBar = dialogView.findViewById(R.id.speed_seekbar);
        Button okButton = dialogView.findViewById(R.id.ok_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        // Tạo mảng speeds mới: 0.25, 0.30, 0.35, ..., 2.0
        float[] speeds = new float[36]; // 36 bước từ 0.25 tới 2.0
        for (int i = 0; i < speeds.length; i++) {
            speeds[i] = 0.25f + (i * 0.05f);
        }

        float currentSpeed = musicService != null ? musicService.getPlaybackSpeed() : 1f;
        int currentIndex = 15; // 1.0 là mặc định (0.25 + 15 * 0.05 = 1.0)
        for (int i = 0; i < speeds.length; i++) {
            if (Math.abs(speeds[i] - currentSpeed) < 0.01) { // So sánh gần đúng vì float
                currentIndex = i;
                break;
            }
        }

        speedSeekBar.setMax(speeds.length - 1); // Cập nhật max cho SeekBar
        speedTitle.setText("Tốc độ: " + String.format("%.2f", speeds[currentIndex]) + "x");
        speedSeekBar.setProgress(currentIndex);

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speedTitle.setText("Tốc độ: " + String.format("%.2f", speeds[progress]) + "x");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        okButton.setOnClickListener(v -> {
            if (musicService != null) {
                float speed = speeds[speedSeekBar.getProgress()];
                musicService.setPlaybackSpeed(speed);
                Toast.makeText(activity, "Tốc độ: " + String.format("%.2f", speed) + "x", Toast.LENGTH_SHORT).show();
                if (onSpeedChanged != null) onSpeedChanged.run();
            }
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }



    public void showTimerDialog(MusicService musicService, long currentTimerMillis, long timerStartTime, Handler handler, Consumer<Long> onTimerSet) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_timer, null);
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView);

        AlertDialog dialog = dialogBuilder.create();

        TextView timerTitle = dialogView.findViewById(R.id.timer_title);
        SeekBar timerSeekBar = dialogView.findViewById(R.id.timer_seekbar);
        TextView remainingTime = dialogView.findViewById(R.id.remaining_time);
        Button okButton = dialogView.findViewById(R.id.ok_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        // Thêm 120 phút vào mảng
        int[] times = {0, 5, 10, 15, 20, 30, 45, 60, 120};
        int currentIndex = 0;
        for (int i = 0; i < times.length; i++) {
            if (currentTimerMillis == TimeUnit.MINUTES.toMillis(times[i])) {
                currentIndex = i;
                break;
            }
        }

        timerSeekBar.setMax(times.length - 1); // Cập nhật max cho SeekBar
        timerTitle.setText(currentIndex == 0 ? "Hẹn giờ: Tắt" : "Hẹn giờ: " + times[currentIndex] + " phút");
        timerSeekBar.setProgress(currentIndex);

        Runnable updateRemainingRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicService != null && currentTimerMillis > 0) {
                    long elapsed = System.currentTimeMillis() - timerStartTime;
                    long remaining = currentTimerMillis - elapsed;
                    if (remaining > 0) {
                        remainingTime.setText("Thời gian còn lại: " + formatDuration((int) remaining));
                        handler.postDelayed(this, 1000);
                    } else {
                        remainingTime.setText("Thời gian còn lại: 0:00");
                    }
                } else {
                    remainingTime.setText("Thời gian còn lại: 0:00");
                }
            }
        };
        handler.post(updateRemainingRunnable);

        timerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timerTitle.setText(progress == 0 ? "Hẹn giờ: Tắt" : "Hẹn giờ: " + times[progress] + " phút");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        okButton.setOnClickListener(v -> {
            if (musicService != null) {
                int minutes = times[timerSeekBar.getProgress()];
                long newTimerMillis = TimeUnit.MINUTES.toMillis(minutes);
                musicService.setTimer(newTimerMillis);
                Toast.makeText(activity, minutes == 0 ? "Hẹn giờ: Tắt" : "Hẹn giờ: " + minutes + " phút", Toast.LENGTH_SHORT).show();
                if (onTimerSet != null) onTimerSet.accept(newTimerMillis);
            }
            handler.removeCallbacks(updateRemainingRunnable);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            handler.removeCallbacks(updateRemainingRunnable);
            dialog.dismiss();
        });

        dialog.show();
    }



    private int getContrastColor(int backgroundColor) {
        double lumBackground = getLuminance(backgroundColor);
        return lumBackground > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private double getLuminance(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;

        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    public ImageView getShuffleButton() { return shuffleButton; }
    public ImageView getPrevButton() { return prevButton; }
    public ImageView getNextButton() { return nextButton; }
    public ImageView getRepeatButton() { return repeatButton; }
    public ImageView getFavoriteButton() { return favoriteButton; }
    public ImageView getSpeedButton() { return speedButton; }
    public ImageView getTimerButton() { return timerButton; }
    public FloatingActionButton getPlayPauseButton() { return playPauseButton; }
    public SeekBar getSeekBar() { return seekBar; }
    public ImageView getCoverArt() { return songImageView; }
    public TextView getSongNameTextView() { return songNameTextView; }
    public TextView getSongArtistTextView() { return songArtistTextView; }
    public TextView getSongAlbumTextView() { return songAlbumTextView; }
}
