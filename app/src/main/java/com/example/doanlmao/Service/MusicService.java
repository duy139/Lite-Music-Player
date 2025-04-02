package com.example.doanlmao.Service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.doanlmao.Adapter.SongAdapter;
import com.example.doanlmao.Model.Song;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MusicService extends Service {
    public MediaPlayer mediaPlayer;
    private final IBinder binder = new MusicBinder();
    private List<Song> songList;
    private int currentSongIndex;
    private List<Integer> shuffledIndices;
    private int currentShuffledPosition = -1;
    private Stack<Integer> historyStack;
    private boolean isRepeating = false;
    private boolean isShuffling = false;
    private float playbackSpeed = 1.0f;
    private long timerMillis = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long timerStartTime = 0;
    private static final String TAG = "MusicService";
    public static final String ACTION_SONG_CHANGED = "com.example.doanlmao.SONG_CHANGED";
    private MusicNotificationManager notificationManager;
    private static final long SONG_CHANGE_DELAY = 500;
    private long lastSongChangeTime = 0;
    private boolean isPreparing = false;
    private boolean wasPlayingBeforeCompletion = false;
    private OnSongChangedListener songChangedListener;
    private Equalizer equalizer;
    private PresetReverb presetReverb;
    private float balance = 0f;
    private float volumeGain = 1.0f;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "EqualizerPrefs";
    private static final int RECENT_SONG_LIMIT = 5;


    public interface OnSongChangedListener {
        void onSongChanged(int newIndex);
    }

    public void setOnSongChangedListener(OnSongChangedListener listener) {
        this.songChangedListener = listener;
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        notificationManager = new MusicNotificationManager(this);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        songList = new ArrayList<>();
        shuffledIndices = new ArrayList<>();
        historyStack = new Stack<>();

        try {
            equalizer = new Equalizer(0, mediaPlayer.getAudioSessionId());
            equalizer.setEnabled(true);
            presetReverb = new PresetReverb(0, mediaPlayer.getAudioSessionId());
            presetReverb.setEnabled(true);

            short savedBass = (short) prefs.getInt("bassLevel", 0);
            short savedMid = (short) prefs.getInt("midLevel", 0);
            short savedTreble = (short) prefs.getInt("trebleLevel", 0);
            balance = prefs.getFloat("balance", 0f);
            volumeGain = prefs.getFloat("volumeGain", 1.0f);
            int reverbLevel = prefs.getInt("reverbLevel", 0);

            equalizer.setBandLevel((short) 0, savedBass);
            equalizer.setBandLevel((short) (equalizer.getNumberOfBands() / 2), savedMid);
            equalizer.setBandLevel((short) (equalizer.getNumberOfBands() - 1), savedTreble);
            setReverbLevel(reverbLevel);
            applyBalanceAndVolume();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Equalizer/PresetReverb: " + e.getMessage());
        }
    }

    public void setReverbLevel(int level) {
        if (presetReverb != null) {
            short preset;
            if (level == 0) {
                preset = PresetReverb.PRESET_NONE;
            } else if (level <= 20) {
                preset = PresetReverb.PRESET_SMALLROOM;
            } else if (level <= 40) {
                preset = PresetReverb.PRESET_MEDIUMROOM;
            } else if (level <= 60) {
                preset = PresetReverb.PRESET_LARGEROOM;
            } else if (level <= 80) {
                preset = PresetReverb.PRESET_MEDIUMHALL;
            } else if (level <= 100) {
                preset = PresetReverb.PRESET_LARGEHALL;
            } else {
                preset = PresetReverb.PRESET_NONE;
            }
            presetReverb.setPreset(preset);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("reverbLevel", level);
            editor.apply();
        }
    }

    public PresetReverb getPresetReverb() {
        return presetReverb;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("PLAY".equals(action)) playSong();
            else if ("PAUSE".equals(action)) pauseSong();
            else if ("NEXT".equals(action)) playNextSong();
            else if ("PREV".equals(action)) playPreviousSong();
        }
        return START_STICKY;
    }

    public void setCurrentSongIndex(int index) {
        if (songList != null && !songList.isEmpty() && index >= 0 && index < songList.size()) {
            this.currentSongIndex = index;
            if (isShuffling) {
                historyStack.push(currentSongIndex); // Lưu bài hiện tại vào lịch sử
                createShuffledList();
                currentShuffledPosition = -1;
            }
            Log.d(TAG, "Set currentSongIndex to: " + index);
            playSong();
        } else {
            Log.e(TAG, "Invalid index or songList: index=" + index + ", songList size=" + (songList != null ? songList.size() : 0));
        }
    }

    public void setSongList(List<Song> songs, int index) {
        this.songList = songs;
        this.currentSongIndex = index >= 0 && index < songs.size() ? index : 0;
        if (isShuffling) {
            createShuffledList();
            historyStack.clear();
            currentShuffledPosition = -1;
        }
        Log.d(TAG, "Set songList size: " + songs.size() + ", currentIndex=" + currentSongIndex);
    }

    public void stopSong() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            notificationManager.hideNotification();
            Intent intent = new Intent(ACTION_SONG_CHANGED);
            intent.putExtra("currentIndex", currentSongIndex);
            sendBroadcast(intent);
            if (songChangedListener != null) songChangedListener.onSongChanged(currentSongIndex);
            Log.d(TAG, "Music stopped completely");
        }
    }

    public void pauseSong() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notificationManager.showNotification(songList.get(currentSongIndex), false, currentSongIndex);
            // Không gửi broadcast ở đây để tránh trigger updateUIForNewSong()
            Log.d(TAG, "Song paused");
        }
    }

    public void resumeSong() {
        if (mediaPlayer != null && songList != null && !songList.isEmpty() && currentSongIndex >= 0 && currentSongIndex < songList.size()) {
            try {
                if (!mediaPlayer.isPlaying()) {
                    if (mediaPlayer.getDuration() == -1) {
                        prepareSong(); // Chuẩn bị lại nếu chưa sẵn sàng
                        mediaPlayer.setOnPreparedListener(mp -> {
                            mp.start();
                            notificationManager.showNotification(songList.get(currentSongIndex), true, currentSongIndex);
                            Log.d(TAG, "Resumed song after preparing: " + songList.get(currentSongIndex).getTitle());
                        });
                    } else {
                        mediaPlayer.start();
                        notificationManager.showNotification(songList.get(currentSongIndex), true, currentSongIndex);
                        Log.d(TAG, "Resumed song: " + songList.get(currentSongIndex).getTitle());
                    }
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error resuming song: " + e.getMessage());
                prepareSong();
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    notificationManager.showNotification(songList.get(currentSongIndex), true, currentSongIndex);
                });
            }
        }
    }

    public void continuePlaying(int index, int position, boolean shouldPlay) {
        if (songList == null || songList.isEmpty() || index < 0 || index >= songList.size()) {
            Log.e(TAG, "Invalid index or songList: index=" + index);
            return;
        }

        currentSongIndex = index;
        if (isShuffling) {
            createShuffledList();
            historyStack.clear();
            currentShuffledPosition = -1;
        }

        try {
            mediaPlayer.reset();
            Uri songUri = Uri.parse(songList.get(index).getPath());
            mediaPlayer.setDataSource(this, songUri);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.seekTo(position);
                mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(playbackSpeed));
                applyBalanceAndVolume();
                if (shouldPlay) {
                    mp.start();
                }
                notificationManager.showNotification(songList.get(currentSongIndex), shouldPlay, currentSongIndex);
                Intent intent = new Intent(ACTION_SONG_CHANGED);
                intent.putExtra("currentIndex", currentSongIndex);
                sendBroadcast(intent);
                if (songChangedListener != null) songChangedListener.onSongChanged(currentSongIndex);
                isPreparing = false;
            });
            mediaPlayer.prepareAsync();
            lastSongChangeTime = System.currentTimeMillis();
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Error in continuePlaying: " + e.getMessage());
            isPreparing = false;
        }
    }


    public void playSong() {
        if (isPreparing) {
            Log.d(TAG, "MediaPlayer is preparing, skipping playSong");
            return;
        }

        if (songList == null || songList.isEmpty() || currentSongIndex < 0 || currentSongIndex >= songList.size()) {
            Log.e(TAG, "Invalid song list or index: " + currentSongIndex);
            return;
        }

        Song song = songList.get(currentSongIndex);
        if (song.getPath() == null) {
            Log.e(TAG, "Null path for song: " + song.getTitle());
            Toast.makeText(this, "Không tìm thấy file: " + song.getTitle(), Toast.LENGTH_SHORT).show();
            playNextSong();
            return;
        }

        try {
            isPreparing = true;
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
            Uri songUri = Uri.parse(song.getPath());
            mediaPlayer.setDataSource(this, songUri);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(playbackSpeed));
                applyBalanceAndVolume();
                mp.start();
                notificationManager.showNotification(song, true, currentSongIndex);
                Intent intent = new Intent(ACTION_SONG_CHANGED);
                intent.putExtra("currentIndex", currentSongIndex);
                sendBroadcast(intent);
                if (songChangedListener != null) songChangedListener.onSongChanged(currentSongIndex);
                isPreparing = false;
                Log.d(TAG, "Playing song: " + song.getTitle());
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Song completed: " + song.getTitle() + ", isRepeating: " + isRepeating);
                isPreparing = false;
                wasPlayingBeforeCompletion = true;
                if (isRepeating) {
                    mp.seekTo(0);
                    mp.start();
                    wasPlayingBeforeCompletion = false;
                } else {
                    playNextSong(true);
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                isPreparing = false;
                if (what != -38) {
                    Toast.makeText(this, "Không phát được bài: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                    playNextSong();
                }
                return true;
            });
            mediaPlayer.prepareAsync();
            lastSongChangeTime = System.currentTimeMillis();
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            isPreparing = false;
            Toast.makeText(this, "Lỗi phát bài: " + song.getTitle(), Toast.LENGTH_SHORT).show();
            playNextSong();
        }
    }




    public void playNextSong() {
        playNextSong(false);
    }

    private void playNextSong(boolean bypassDelay) {
        long currentTime = System.currentTimeMillis();
        if (!bypassDelay && (currentTime - lastSongChangeTime < SONG_CHANGE_DELAY)) {
            Log.d(TAG, "Skipping playNextSong due to debounce");
            return;
        }
        if (songList == null || songList.isEmpty()) {
            Log.e(TAG, "Song list is null or empty");
            return;
        }

        if (isRepeating) {
            playSong();
            return;
        }

        if (isShuffling) {
            if (currentSongIndex >= 0) {
                historyStack.push(currentSongIndex);
            }
            currentShuffledPosition++;
            if (currentShuffledPosition >= shuffledIndices.size() || currentShuffledPosition < 0) {
                createShuffledList();
                currentShuffledPosition = 0;
            }
            currentSongIndex = shuffledIndices.get(currentShuffledPosition);
        } else {
            currentSongIndex = (currentSongIndex + 1) % songList.size();
            currentShuffledPosition = -1;
            historyStack.clear();
        }
        Log.d(TAG, "Next song index: " + currentSongIndex);

        // Chuẩn bị bài mới dù đang paused
        prepareSong();
        // Gửi broadcast để cập nhật UI
        Intent intent = new Intent(ACTION_SONG_CHANGED);
        intent.putExtra("currentIndex", currentSongIndex);
        sendBroadcast(intent);
        if (songChangedListener != null) songChangedListener.onSongChanged(currentSongIndex);
        lastSongChangeTime = System.currentTimeMillis();
    }

    public void playPreviousSong() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSongChangeTime < SONG_CHANGE_DELAY) {
            Log.d(TAG, "Skipping playPreviousSong due to debounce");
            return;
        }
        if (songList == null || songList.isEmpty()) {
            Log.e(TAG, "Song list is null or empty");
            return;
        }

        if (isShuffling) {
            if (!historyStack.isEmpty()) {
                currentSongIndex = historyStack.pop();
                currentShuffledPosition = shuffledIndices.indexOf(currentSongIndex);
            } else {
                currentShuffledPosition = 0;
                currentSongIndex = shuffledIndices.get(currentShuffledPosition);
            }
        } else {
            currentSongIndex--;
            if (currentSongIndex < 0) currentSongIndex = songList.size() - 1;
            currentShuffledPosition = -1;
            historyStack.clear();
        }
        Log.d(TAG, "Previous song index: " + currentSongIndex);

        // Chuẩn bị bài mới dù đang paused
        prepareSong();
        // Gửi broadcast để cập nhật UI
        Intent intent = new Intent(ACTION_SONG_CHANGED);
        intent.putExtra("currentIndex", currentSongIndex);
        sendBroadcast(intent);
        if (songChangedListener != null) songChangedListener.onSongChanged(currentSongIndex);
        lastSongChangeTime = System.currentTimeMillis();
    }

    // Hàm mới để chuẩn bị bài hát mà không phát ngay
    private void prepareSong() {
        if (isPreparing) {
            Log.d(TAG, "MediaPlayer is preparing, skipping prepareSong");
            return;
        }

        if (songList == null || songList.isEmpty() || currentSongIndex < 0 || currentSongIndex >= songList.size()) {
            Log.e(TAG, "Invalid song list or index: " + currentSongIndex);
            return;
        }

        Song song = songList.get(currentSongIndex);
        if (song.getPath() == null) {
            Log.e(TAG, "Null path for song: " + song.getTitle());
            Toast.makeText(this, "Không tìm thấy file: " + song.getTitle(), Toast.LENGTH_SHORT).show();
            playNextSong();
            return;
        }

        try {
            isPreparing = true;
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
            Uri songUri = Uri.parse(song.getPath());
            mediaPlayer.setDataSource(this, songUri);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(playbackSpeed));
                applyBalanceAndVolume();
                // Không start ngay, chỉ chuẩn bị
                notificationManager.showNotification(song, isPlaying(), currentSongIndex);
                Intent intent = new Intent(ACTION_SONG_CHANGED);
                intent.putExtra("currentIndex", currentSongIndex);
                sendBroadcast(intent);
                if (songChangedListener != null) songChangedListener.onSongChanged(currentSongIndex);
                isPreparing = false;
                Log.d(TAG, "Prepared song: " + song.getTitle() + " (not playing yet)");
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Song completed: " + song.getTitle());
                isPreparing = false;
                wasPlayingBeforeCompletion = true;
                if (isRepeating) {
                    mp.seekTo(0);
                    mp.start();
                    wasPlayingBeforeCompletion = false;
                } else {
                    playNextSong(true);
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                isPreparing = false;
                if (what != -38) {
                    Toast.makeText(this, "Không phát được bài: " + song.getTitle(), Toast.LENGTH_SHORT).show();
                    playNextSong();
                }
                return true;
            });
            mediaPlayer.prepareAsync();
            lastSongChangeTime = System.currentTimeMillis();
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            isPreparing = false;
            Toast.makeText(this, "Lỗi chuẩn bị bài: " + song.getTitle(), Toast.LENGTH_SHORT).show();
            playNextSong();
        }
    }

    private void createShuffledList() {
        if (songList == null || songList.isEmpty()) return;

        shuffledIndices.clear();
        for (int i = 0; i < songList.size(); i++) {
            shuffledIndices.add(i);
        }

        // Random toàn bộ danh sách, không giới hạn RECENT_SONG_LIMIT
        Collections.shuffle(shuffledIndices, new Random());

        // Đảm bảo bài hiện tại (nếu có) không bị lặp ngay đầu danh sách
        if (currentSongIndex >= 0 && shuffledIndices.get(0).equals(currentSongIndex)) {
            int swapIndex = new Random().nextInt(shuffledIndices.size() - 1) + 1;
            Collections.swap(shuffledIndices, 0, swapIndex);
        }

        Log.d(TAG, "Created new shuffled list: " + shuffledIndices);
    }

    public void setShuffling(boolean shuffling) {
        this.isShuffling = shuffling;
        if (shuffling) {
            createShuffledList();
            historyStack.clear();
            currentShuffledPosition = -1;
        } else {
            shuffledIndices.clear();
            currentShuffledPosition = -1;
            historyStack.clear();
        }
        Log.d(TAG, "Shuffling set to: " + isShuffling);
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
        if (mediaPlayer == null) {
            Log.w(TAG, "MediaPlayer is null, cannot set playback speed");
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                // Lưu trạng thái hiện tại
                int currentPosition = mediaPlayer.getCurrentPosition();
                Song currentSong = songList.get(currentSongIndex);

                // Pause và reset MediaPlayer
                mediaPlayer.pause();
                mediaPlayer.reset();

                // Cấu hình lại MediaPlayer với bài hát hiện tại
                Uri songUri = Uri.parse(currentSong.getPath());
                mediaPlayer.setDataSource(this, songUri);
                mediaPlayer.setOnPreparedListener(mp -> {
                    // Set tốc độ mới
                    PlaybackParams params = mp.getPlaybackParams();
                    params.setSpeed(speed);
                    mp.setPlaybackParams(params);

                    // Áp dụng lại âm lượng và balance
                    applyBalanceAndVolume();

                    // Quay lại vị trí cũ và phát tiếp
                    mp.seekTo(currentPosition);
                    mp.start();

                    Log.d(TAG, "Playback speed set to " + speed + "x and resumed at position " + currentPosition);
                });
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "Error after setting playback speed: what=" + what + ", extra=" + extra);
                    return true;
                });
                mediaPlayer.prepareAsync();
            } else {
                // Nếu không đang phát, chỉ set tốc độ để áp dụng khi phát lần sau
                PlaybackParams params = mediaPlayer.getPlaybackParams();
                params.setSpeed(speed);
                mediaPlayer.setPlaybackParams(params);
                applyBalanceAndVolume();
                Log.d(TAG, "Playback speed set to " + speed + "x (not playing)");
            }
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Error setting playback speed: " + e.getMessage());
            // Nếu lỗi, thử khôi phục trạng thái mặc định
            try {
                mediaPlayer.reset();
                Uri songUri = Uri.parse(songList.get(currentSongIndex).getPath());
                mediaPlayer.setDataSource(this, songUri);
                mediaPlayer.prepare();
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
                applyBalanceAndVolume();
                mediaPlayer.start();
            } catch (IOException ex) {
                Log.e(TAG, "Failed to recover MediaPlayer: " + ex.getMessage());
                Toast.makeText(this, "Lỗi khi thay đổi tốc độ phát", Toast.LENGTH_SHORT).show();
            }
        }
    }





    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setTimer(long millis) {
        timerHandler.removeCallbacksAndMessages(null);
        this.timerMillis = millis;
        if (millis > 0) {
            this.timerStartTime = System.currentTimeMillis();
            timerHandler.postDelayed(() -> {
                pauseSong();
                timerMillis = 0;
                timerStartTime = 0;
            }, millis);
        } else {
            this.timerStartTime = 0;
        }
    }

    public long getTimerMillis() {
        return timerMillis;
    }

    public long getTimerStartTime() {
        return timerStartTime;
    }

    public Equalizer getEqualizer() {
        return equalizer;
    }

    public void setBalance(float balance) {
        this.balance = balance;
        applyBalanceAndVolume();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("balance", balance);
        editor.apply();
    }

    public float getBalance() {
        return balance;
    }

    public void setVolumeGain(float gain) {
        this.volumeGain = gain;
        applyBalanceAndVolume();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("volumeGain", gain);
        editor.apply();
    }

    public float getVolumeGain() {
        return volumeGain;
    }

    private void applyBalanceAndVolume() {
        if (mediaPlayer != null) {
            float leftVolume = volumeGain * (balance <= 0 ? 1.0f : 1.0f - balance);
            float rightVolume = volumeGain * (balance >= 0 ? 1.0f : 1.0f + balance);
            leftVolume = Math.max(0.0f, Math.min(1.0f, leftVolume));
            rightVolume = Math.max(0.0f, Math.min(1.0f, rightVolume));
            mediaPlayer.setVolume(leftVolume, rightVolume);
            Log.d(TAG, "Applied volume - Left: " + leftVolume + ", Right: " + rightVolume);
        } else {
            Log.w(TAG, "MediaPlayer is null, cannot apply balance and volume");
        }
    }



    public void setRepeating(boolean repeating) {
        isRepeating = repeating;
    }



    public boolean isRepeating() {
        return isRepeating;
    }

    public boolean isShuffling() {
        return isShuffling;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentSongIndex() {
        return currentSongIndex;
    }

    public List<Song> getSongList() {
        return songList;
    }

    public int getAudioSessionId() {
        return mediaPlayer != null ? mediaPlayer.getAudioSessionId() : 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        if (presetReverb != null) {
            presetReverb.release();
            presetReverb = null;
        }
        timerHandler.removeCallbacksAndMessages(null);
    }
}