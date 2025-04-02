package com.example.doanlmao.Activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doanlmao.Helper.PlayerUIHelper;
import com.example.doanlmao.Misc.SwipeGestureHandler;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.example.doanlmao.Service.MusicService;
import com.example.doanlmao.Database.FavoriteDatabase;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {
    private PlayerUIHelper uiHelper;
    private MusicService musicService;
    private boolean isBound = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private FavoriteDatabase favoriteDatabase;
    private long timerStartTime = 0;
    private long currentTimerMillis = 0;
    private BroadcastReceiver songChangeReceiver;
    private static final String TAG = "PlayerActivity";
    private SwipeGestureHandler swipeGestureHandler;
    private boolean isUpdatingUI = false;
    private boolean lastPlayingState = false; // Lưu trạng thái phát trước đó

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Log.d(TAG, "onCreate: Orientation = " + getResources().getConfiguration().orientation);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setFullScreenMode(true);
        } else {
            setFullScreenMode(false);
        }

        uiHelper = new PlayerUIHelper(this);
        favoriteDatabase = new FavoriteDatabase(this);

        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        setupButtons();
        setupSwipeGestureHandler();
        setupSongChangeReceiver();
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;

            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("currentIndex")) {
                int indexFromIntent = intent.getIntExtra("currentIndex", -1);
                int currentPosition = intent.getIntExtra("currentPosition", 0);
                boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
                if (indexFromIntent >= 0) {
                    musicService.continuePlaying(indexFromIntent, currentPosition, isPlaying);
                }
            }

            currentTimerMillis = musicService.getTimerMillis();
            timerStartTime = musicService.getTimerStartTime();

            initializeUI();
            startSeekBarUpdate();
            musicService.setOnSongChangedListener(newIndex -> {
                Log.d(TAG, "OnSongChangedListener triggered: " + newIndex);
                updateUIForNewSong();
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged: New orientation = " + newConfig.orientation);

        Song currentSong = null;
        if (musicService != null && musicService.getSongList() != null && musicService.getCurrentSongIndex() >= 0) {
            currentSong = musicService.getSongList().get(musicService.getCurrentSongIndex());
        }

        setContentView(R.layout.activity_player);
        uiHelper = new PlayerUIHelper(this);
        setupButtons();
        setupSwipeGestureHandler();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setFullScreenMode(true);
            Log.d(TAG, "Switched to Landscape - Full screen enabled");
        } else {
            setFullScreenMode(false);
            Log.d(TAG, "Switched to Portrait - Full screen disabled");
        }

        if (currentSong != null) {
            uiHelper.updateSongUI(currentSong);
            updateUIForNewSong();
        } else {
            initializeUI();
        }
    }

    private void initializeUI() {
        if (musicService != null && musicService.getSongList() != null && musicService.getCurrentSongIndex() >= 0) {
            updateUIForNewSong();
            lastPlayingState = musicService.isPlaying(); // Lưu trạng thái ban đầu
        }
    }

    private void setFullScreenMode(boolean enable) {
        Window window = getWindow();
        if (enable) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void setupSongChangeReceiver() {
        songChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (MusicService.ACTION_SONG_CHANGED.equals(intent.getAction())) {
                    Log.d(TAG, "Broadcast received: ACTION_SONG_CHANGED");
                    updateUIForNewSong();
                }
            }
        };
        IntentFilter filter = new IntentFilter(MusicService.ACTION_SONG_CHANGED);
        registerReceiver(songChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void setupSwipeGestureHandler() {
        int orientation = getResources().getConfiguration().orientation;
        View swipeView = orientation == Configuration.ORIENTATION_LANDSCAPE ?
                findViewById(R.id.cover_art) : findViewById(R.id.swipe_area);

        if (swipeView == null) {
            Log.e(TAG, "Đm, swipeView null, kiểm tra layout đi!");
            return;
        }

        View rootView = findViewById(R.id.mContainer);

        SwipeGestureHandler.OnSwipeListener swipeListener = new SwipeGestureHandler.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                playNext();
            }

            @Override
            public void onSwipeRight() {
                playPrevious();
            }

            @Override
            public void onSwipeDown() {
                finish();
            }
        };
        swipeGestureHandler = new SwipeGestureHandler(
                swipeView,
                uiHelper.getCoverArt(),
                uiHelper.getSongNameTextView(),
                uiHelper.getSongArtistTextView(),
                uiHelper.getSongAlbumTextView(),
                rootView,
                swipeListener
        );
    }

    private void updateUIForNewSong() {
        if (musicService != null && musicService.getSongList() != null && musicService.getCurrentSongIndex() >= 0) {
            Song currentSong = musicService.getSongList().get(musicService.getCurrentSongIndex());
            Log.d(TAG, "Updating UI for song: " + currentSong.getTitle());
            uiHelper.updateSongUI(currentSong);
            uiHelper.updateAllUIStates(
                    musicService.isPlaying(),
                    musicService.isShuffling(),
                    musicService.isRepeating(),
                    favoriteDatabase.isFavorite(currentSong.getPath())
            );
            updateSeekBarImmediately();
        } else {
            Log.w(TAG, "Cannot update UI: musicService or songList is null");
        }
    }

    private void setupButtons() {
        uiHelper.getPlayPauseButton().setOnClickListener(v -> togglePlayPause());
        uiHelper.getPrevButton().setOnClickListener(v -> playPrevious());
        uiHelper.getNextButton().setOnClickListener(v -> playNext());
        uiHelper.getShuffleButton().setOnClickListener(v -> toggleShuffle());
        uiHelper.getRepeatButton().setOnClickListener(v -> toggleRepeat());
        uiHelper.getFavoriteButton().setOnClickListener(v -> toggleFavorite());
        uiHelper.getSpeedButton().setOnClickListener(v -> uiHelper.showSpeedDialog(musicService, null));
        uiHelper.getTimerButton().setOnClickListener(v -> uiHelper.showTimerDialog(
                musicService,
                currentTimerMillis,
                timerStartTime,
                handler,
                newTimerMillis -> {
                    currentTimerMillis = newTimerMillis;
                    timerStartTime = System.currentTimeMillis();
                    if (musicService != null) {
                        musicService.setTimer(newTimerMillis);
                    }
                }
        ));

        uiHelper.getSeekBar().setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    uiHelper.updateSeekBar(progress, musicService.getDuration());
                    seekBar.setThumb(null);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBar.setThumb(null);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (musicService != null && musicService.mediaPlayer != null) {
                    musicService.mediaPlayer.seekTo(seekBar.getProgress());
                }
                seekBar.setThumb(null);
            }
        });
    }

    private void togglePlayPause() {
        if (musicService != null) {
            if (musicService.isPlaying()) {
                musicService.pauseSong();
            } else {
                musicService.resumeSong();
            }
            // Cập nhật icon ngay lập tức và lưu trạng thái
            uiHelper.updatePlayPauseIcon(musicService.isPlaying());
            lastPlayingState = musicService.isPlaying();
        }
    }

    private void playPrevious() {
        if (musicService != null) {
            Log.d(TAG, "playPrevious called");
            musicService.playPreviousSong();
        }
    }

    private void playNext() {
        if (musicService != null) {
            Log.d(TAG, "playNext called");
            musicService.playNextSong();
        }
    }

    private void toggleShuffle() {
        if (musicService != null) {
            boolean isShuffling = !musicService.isShuffling();
            musicService.setShuffling(isShuffling);
            uiHelper.updateShuffleIcon(isShuffling);
            Toast.makeText(this, isShuffling ? "Phát ngẫu nhiên: Bật" : "Phát ngẫu nhiên: Tắt", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleRepeat() {
        if (musicService != null) {
            boolean isRepeating = !musicService.isRepeating();
            musicService.setRepeating(isRepeating);
            uiHelper.updateRepeatIcon(isRepeating);
            Toast.makeText(this, isRepeating ? "Lặp bài: Bật" : "Lặp bài: Tắt", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        if (musicService != null && musicService.getSongList() != null) {
            Song currentSong = musicService.getSongList().get(musicService.getCurrentSongIndex());
            if (favoriteDatabase.isFavorite(currentSong.getPath())) {
                favoriteDatabase.removeFavorite(currentSong.getPath());
                Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                favoriteDatabase.addFavorite(currentSong);
                Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            }
            uiHelper.updateFavoriteIcon(favoriteDatabase.isFavorite(currentSong.getPath()));
        }
    }

    private void updateSeekBarImmediately() {
        if (musicService != null && musicService.mediaPlayer != null && !isUpdatingUI) {
            isUpdatingUI = true;
            uiHelper.updateSeekBar(musicService.getCurrentPosition(), musicService.getDuration());
            uiHelper.updateAllUIStates(
                    musicService.isPlaying(),
                    musicService.isShuffling(),
                    musicService.isRepeating(),
                    favoriteDatabase.isFavorite(musicService.getSongList().get(musicService.getCurrentSongIndex()).getPath())
            );
            isUpdatingUI = false;
        }
    }

    private void startSeekBarUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (musicService != null && musicService.mediaPlayer != null && !isUpdatingUI) {
                    isUpdatingUI = true;
                    uiHelper.updateSeekBar(musicService.getCurrentPosition(), musicService.getDuration());
                    // Chỉ cập nhật icon nếu trạng thái thay đổi
                    boolean currentPlayingState = musicService.isPlaying();
                    if (currentPlayingState != lastPlayingState) {
                        uiHelper.updatePlayPauseIcon(currentPlayingState);
                        lastPlayingState = currentPlayingState;
                    }
                    isUpdatingUI = false;
                }
                handler.postDelayed(this, 100);
            }
        }, 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        if (songChangeReceiver != null) {
            unregisterReceiver(songChangeReceiver);
        }
        handler.removeCallbacksAndMessages(null);
    }
}