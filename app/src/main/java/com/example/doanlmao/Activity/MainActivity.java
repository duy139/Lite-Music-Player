package com.example.doanlmao.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanlmao.Database.FirebaseSyncManager;
import com.example.doanlmao.Database.PlaylistDatabase;
import com.example.doanlmao.Fragment.AboutFragment;
import com.example.doanlmao.Fragment.AlbumFragment;
import com.example.doanlmao.Fragment.EqualizerFragment;
import com.example.doanlmao.Fragment.FavoriteFragment;
import com.example.doanlmao.Fragment.PlaylistFragment;
import com.example.doanlmao.Fragment.SearchFragment;
import com.example.doanlmao.Fragment.SettingFragment;
import com.example.doanlmao.Fragment.SongFragment;
import com.example.doanlmao.MiniPlayerController;
import com.example.doanlmao.Model.MusicViewModel;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.example.doanlmao.Service.MusicService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private FrameLayout frameLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    public MiniPlayerController miniPlayerController;
    public static MusicService musicService;
    private boolean isBound = false;
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 101;

    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private ImageView miniPlayerArtwork;

    private SongFragment songFragment;
    private AlbumFragment albumFragment;
    private SearchFragment searchFragment;
    private PlaylistFragment playlistFragment;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private MusicViewModel viewModel;
    private SharedPreferences prefs; // Thêm SharedPreferences
    private static final String PREFS_NAME = "MusicPlayerPrefs";
    private static final String KEY_SONG_INDEX = "song_index";
    private static final String KEY_POSITION = "song_position";
    private static final String KEY_IS_PLAYING = "is_playing";
    private BroadcastReceiver scanFinishedReceiver;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            miniPlayerController.updateMiniPlayer();
            miniPlayerController.setControllable(true);
            musicService.setOnSongChangedListener(newIndex -> {
                runOnUiThread(() -> {
                    if (newIndex >= 0 && newIndex < musicService.getSongList().size()) {
                        Song song = musicService.getSongList().get(newIndex);
                        updatePlayerUI(song.getTitle(), song.getArtist(), song.getArtwork());
                    }
                });
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        DynamicColors.applyToActivityIfAvailable(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        frameLayout = findViewById(R.id.frameLayout);
        navigationView = findViewById(R.id.navigationView);
        drawerLayout = findViewById(R.id.main);

        miniPlayerController = new MiniPlayerController(this);

        viewModel = new ViewModelProvider(this).get(MusicViewModel.class);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Thay bằng client ID của bạn từ Firebase Console, hoặc để vậy nếu bạn tự thêm google-services.json
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initializeGoogleSignInLauncher();

        checkAndRequestPermissions();

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        startService(intent);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        songFragment = new SongFragment();
        albumFragment = new AlbumFragment();
        searchFragment = new SearchFragment();
        playlistFragment = new PlaylistFragment();

        setupScanFinishedReceiver();

        if (savedInstanceState == null) {
            loadFragment(songFragment, false);
            bottomNavigationView.setSelectedItemId(R.id.bottom_nav_song);
            navigationView.setCheckedItem(R.id.header_nav_home);
            toolbar.setTitle("Danh sách nhạc");
            restoreMusicState();
            miniPlayerController.updateMiniPlayer();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottom_nav_song) {
                loadFragment(songFragment, false);
                navigationView.setCheckedItem(R.id.header_nav_home);
                toolbar.setTitle("Danh sách nhạc");
                toolbar.getMenu().clear();
                getMenuInflater().inflate(R.menu.toolbar_menu, toolbar.getMenu());
            } else if (itemId == R.id.bottom_nav_album) {
                loadFragment(albumFragment, false);
                toolbar.setTitle("Album");
                toolbar.getMenu().clear();
            } else if (itemId == R.id.bottom_nav_search) {
                loadFragment(searchFragment, false);
                toolbar.setTitle("Tìm kiếm");
                toolbar.getMenu().clear();
            } else if (itemId == R.id.bottom_nav_equalizer) {
                // Bỏ điều kiện đăng nhập, load thẳng EqualizerFragment
                loadFragment(new EqualizerFragment(), false);
                toolbar.setTitle("Bộ chỉnh âm");
                toolbar.getMenu().clear();
            }
            miniPlayerController.updateMiniPlayer();
            return true;
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.header_nav_home) {
                loadFragment(songFragment, false);
                bottomNavigationView.setSelectedItemId(R.id.bottom_nav_song);
                toolbar.setTitle("Danh sách nhạc");
                toolbar.getMenu().clear();
                getMenuInflater().inflate(R.menu.toolbar_menu, toolbar.getMenu());
            } else if (itemId == R.id.header_nav_favoriteSongs) {
                loadFragment(new FavoriteFragment(), false);
                toolbar.setTitle("Nhạc yêu thích");
                toolbar.getMenu().clear();
            } else if (itemId == R.id.header_nav_playlist) {
                loadFragment(playlistFragment, false);
                toolbar.setTitle("Playlist");
                toolbar.getMenu().clear();
            } else if (itemId == R.id.header_nav_settings) {
                loadFragment(new SettingFragment(), false);
                toolbar.setTitle("Cài đặt");
                toolbar.getMenu().clear();
            } else if (itemId == R.id.header_nav_about) {
                loadFragment(new AboutFragment(), false);
                toolbar.setTitle("About");
                toolbar.getMenu().clear();
            }
            miniPlayerController.updateMiniPlayer();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }


    private void loadFragment(Fragment fragment, boolean isAppInitialized) {
        Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (isAppInitialized && getSupportFragmentManager().findFragmentById(R.id.frameLayout) == null) {
            transaction.add(R.id.frameLayout, fragment);
        } else {
            transaction.replace(R.id.frameLayout, fragment);
        }
        transaction.commit();
        Log.d(TAG, "Fragment loaded: " + fragment.getClass().getSimpleName());
    }

    private void initializeGoogleSignInLauncher() {
        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(this, "Google Sign-In thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Đăng nhập bị hủy", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoginDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng nhập để sử dụng")
                .setMessage("Bạn cần đăng nhập để sử dụng bộ chỉnh âm. Đăng nhập ngay?")
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    if (googleSignInLauncher != null) {
                        googleSignInLauncher.launch(signInIntent);
                    } else {
                        Toast.makeText(this, "Lỗi khởi tạo đăng nhập, vui lòng thử lại!", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "googleSignInLauncher is null in showLoginDialog");
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Đăng nhập thành công với Google", Toast.LENGTH_SHORT).show();
                        loadFragment(new EqualizerFragment(), false);
                        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_equalizer);
                        toolbar.setTitle("Bộ chỉnh âm");

                        // Sync lần đầu khi đăng nhập
                        PlaylistDatabase playlistDb = new PlaylistDatabase(this);
                        FirebaseSyncManager syncManager = new FirebaseSyncManager(playlistDb);
                        syncManager.fetchPlaylistsFromFirebase(() -> {
                            if (playlistFragment != null) {
                                playlistFragment.loadPlaylists();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }



    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void updatePlayerUI(String title, String artist, byte[] artwork) {
        Log.d(TAG, "Updating Player UI: Title = " + title + ", Artist = " + artist);
        if (miniPlayerTitle != null) miniPlayerTitle.setText(title);
        if (miniPlayerArtist != null) miniPlayerArtist.setText(artist);
        if (miniPlayerArtwork != null) {
            if (artwork != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork.length);
                miniPlayerArtwork.setImageBitmap(bitmap);
            } else {
                miniPlayerArtwork.setImageResource(R.drawable.baseline_music_note_24);
            }
        }
    }

    public boolean isServiceBound() {
        return isBound;
    }

    private void checkAndRequestPermissions() {
        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS} :
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.POST_NOTIFICATIONS};

        if (Arrays.stream(permissions).anyMatch(perm -> ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "All required permissions already granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && Arrays.stream(grantResults).allMatch(result -> result == PackageManager.PERMISSION_GRANTED)) {
            Log.d(TAG, "All permissions granted");
        } else {
            Toast.makeText(this, "Cần quyền để truy cập nhạc và thông báo", Toast.LENGTH_LONG).show();
        }
    }

    public void updateMiniPlayerAfterSongChange() {
        if (miniPlayerController != null) {
            miniPlayerController.updateMiniPlayer();
        }
    }
    private void setupScanFinishedReceiver() {
        scanFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (MusicViewModel.ACTION_SCAN_FINISHED.equals(intent.getAction())) {
                    Log.d(TAG, "Scan finished received, hiding MiniPlayer");
                    miniPlayerController.setJustScanned(true);
                    miniPlayerController.updateMiniPlayer(); // Cập nhật ngay để ẩn
                }
            }
        };
        IntentFilter filter = new IntentFilter(MusicViewModel.ACTION_SCAN_FINISHED);
        // Thêm flag RECEIVER_NOT_EXPORTED vì broadcast chỉ dùng nội bộ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanFinishedReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(scanFinishedReceiver, filter);
        }
    }

    private void restoreMusicState() {
        if (isBound && musicService != null) {
            int savedIndex = prefs.getInt(KEY_SONG_INDEX, 0);
            int savedPosition = prefs.getInt(KEY_POSITION, 0);
            boolean wasPlaying = prefs.getBoolean(KEY_IS_PLAYING, false);

            List<Song> songs = viewModel.getSongs().getValue();
            if (songs != null && !songs.isEmpty()) {
                musicService.setSongList(songs, savedIndex >= 0 && savedIndex < songs.size() ? savedIndex : 0);
                if (savedIndex >= 0 && savedIndex < songs.size()) {
                    musicService.continuePlaying(savedIndex, savedPosition, false);
                    miniPlayerController.updateMiniPlayer();
                } else {
                    musicService.setSongList(songs, 0);
                }
                Log.d(TAG, "Restored state: index=" + savedIndex + ", position=" + savedPosition + ", wasPlaying=" + wasPlaying);
            } else {
                Log.w(TAG, "No songs available to restore");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound && musicService != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_SONG_INDEX, musicService.getCurrentSongIndex());
            editor.putInt(KEY_POSITION, musicService.getCurrentPosition());
            editor.putBoolean(KEY_IS_PLAYING, musicService.isPlaying());
            editor.apply();
            Log.d(TAG, "Saved state before destroy: index=" + musicService.getCurrentSongIndex() + ", position=" + musicService.getCurrentPosition());
        }
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        if (scanFinishedReceiver != null) {
            unregisterReceiver(scanFinishedReceiver);
        }
        miniPlayerController.cleanup();
    }

}

