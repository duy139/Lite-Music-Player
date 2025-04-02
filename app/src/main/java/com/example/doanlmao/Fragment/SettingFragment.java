package com.example.doanlmao.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Database.DatabaseHelper;
import com.example.doanlmao.Helper.DriveServiceHelper;
import com.example.doanlmao.Helper.SettingUIHelper;
import com.example.doanlmao.Model.MusicViewModel;
import com.example.doanlmao.Model.Song;
import com.example.doanlmao.R;
import com.example.doanlmao.Service.BackupRestoreManager;
import com.example.doanlmao.Service.SongScanner;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class SettingFragment extends Fragment implements SettingUIHelper.SettingFragmentCallbacks {
    private SwitchMaterial darkSwitch, autoSwitch;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_MODE = "themeMode";
    private static final String KEY_AUTO = "autoMode";
    private static final String KEY_SCAN_INTERVAL = "scanInterval";
    private SongScanner songScanner;
    private Handler scanHandler = new Handler();
    private Runnable scanRunnable;
    private ActivityResultLauncher<Intent> folderPickerLauncher;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private SettingUIHelper uiHelper;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ImageView userPhoto;
    private TextView userName;
    private MaterialButton loginButton, logoutButton;
    private View userInfoLayout;
    private MaterialButton backupMusicButton, restoreMusicButton;
    private DriveServiceHelper driveServiceHelper;
    private BackupRestoreManager backupRestoreManager;

    private boolean isDriveInitFailed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        songScanner = new SongScanner(requireContext(), new DatabaseHelper(requireContext()));
        uiHelper = new SettingUIHelper(requireContext(), this);
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Thay bằng client ID của bạn từ Firebase Console hoặc để vậy nếu bạn tự thêm google-services.json
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.file"))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                    Log.d("SettingFragment", "Google Sign-In scopes: " + account.getGrantedScopes());
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(requireContext(), "Google Sign-In thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(requireContext(), "Đăng nhập bị hủy", Toast.LENGTH_SHORT).show();
            }
        });

        folderPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri treeUri = result.getData().getData();
                if (treeUri != null) {
                    requireActivity().getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    songScanner.saveTreeUri(treeUri);
                    scanSongsFromFolder(treeUri); // Quét folder mới
                } else {
                    Toast.makeText(requireContext(), "Không lấy được đường dẫn thư mục!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DynamicColors.applyToActivitiesIfAvailable(requireActivity().getApplication());
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        darkSwitch = view.findViewById(R.id.dark_switch);
        autoSwitch = view.findViewById(R.id.auto_switch);
        MaterialButton rescanFolderButton = view.findViewById(R.id.rescan_folder_button);
        MaterialButton autoScanButton = view.findViewById(R.id.auto_scan_button);
        loginButton = view.findViewById(R.id.login_button);
        userInfoLayout = view.findViewById(R.id.user_info_layout);
        userPhoto = view.findViewById(R.id.user_photo);
        userName = view.findViewById(R.id.user_name);
        logoutButton = view.findViewById(R.id.logout_button);
        backupMusicButton = view.findViewById(R.id.backup_music_button);
        restoreMusicButton = view.findViewById(R.id.restore_music_button);

        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int defaultThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        int themeMode = prefs.getInt(KEY_MODE, defaultThemeMode);
        boolean autoMode = prefs.getBoolean(KEY_AUTO, true);

        setSwitchState(themeMode, autoMode);
        applyTheme(themeMode, autoMode);

        darkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!autoSwitch.isChecked()) {
                saveAndApplyTheme(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO, false);
            }
        });

        autoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                saveAndApplyTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, true);
            } else {
                saveAndApplyTheme(AppCompatDelegate.MODE_NIGHT_NO, false);
                darkSwitch.setChecked(false);
            }
        });

        rescanFolderButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            folderPickerLauncher.launch(intent);
        });

        autoScanButton.setOnClickListener(v -> uiHelper.showAutoScanDialog(prefs.getInt(KEY_SCAN_INTERVAL, 0), this::setupAutoScan));

        loginButton.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        logoutButton.setOnClickListener(v -> logoutUser());

        backupMusicButton.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để sao lưu!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (driveServiceHelper == null || backupRestoreManager == null) {
                Toast.makeText(requireContext(), "Đang kết nối Drive, chờ chút...", Toast.LENGTH_SHORT).show();
                initDriveServiceAsync();
                return;
            }
            showSongSelectionDialog();
        });

        restoreMusicButton.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để khôi phục!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (driveServiceHelper == null || backupRestoreManager == null) {
                Toast.makeText(requireContext(), "Đang kết nối Drive, chờ chút...", Toast.LENGTH_SHORT).show();
                initDriveServiceAsync();
                return;
            }
            backupRestoreManager.restoreSongs(() -> notifySongFragment());
        });

        if (mAuth.getCurrentUser() != null && driveServiceHelper == null) {
            initDriveServiceAsync();
        } else {
            backupMusicButton.setEnabled(false);
            restoreMusicButton.setEnabled(false);
        }

        updateUserInfo();
        setupAutoScan();
        return view;
    }

    private void initDriveServiceAsync() {
        new Thread(() -> {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account == null) {
                Log.w("SettingFragment", "No Google account found, prompting sign-in");
                if (!isDriveInitFailed) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Vui lòng đăng nhập tài khoản Google!", Toast.LENGTH_SHORT).show());
                    isDriveInitFailed = true;
                }
                return;
            }

            Log.d("SettingFragment", "Initializing Drive with account: " + account.getEmail() + ", scopes: " + account.getGrantedScopes());
            if (!account.getGrantedScopes().contains(new Scope("https://www.googleapis.com/auth/drive.file"))) {
                Log.w("SettingFragment", "Missing drive.file scope, forcing re-auth");
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                });
                return;
            }

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), Collections.singleton("https://www.googleapis.com/auth/drive.file"));
            credential.setSelectedAccount(account.getAccount());

            try {
                if (!isNetworkAvailable()) {
                    throw new IOException("No network connection");
                }
                NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                Drive googleDriveService = new Drive.Builder(
                        httpTransport,
                        new GsonFactory(),
                        credential)
                        .setApplicationName("DoAnLmao")
                        .build();
                driveServiceHelper = new DriveServiceHelper(googleDriveService);
                backupRestoreManager = new BackupRestoreManager(requireContext(), driveServiceHelper);
                requireActivity().runOnUiThread(() -> {
                    backupMusicButton.setEnabled(true);
                    restoreMusicButton.setEnabled(true);
                    isDriveInitFailed = false;
                    Toast.makeText(requireContext(), "Đã kết nối Google Drive!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("SettingFragment", "Failed to initialize Google Drive service: " + e.getMessage(), e);
                if (!isDriveInitFailed) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Không thể kết nối Drive: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    isDriveInitFailed = true;
                }
                driveServiceHelper = null;
                backupRestoreManager = null;
            }
        }).start();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void showSongSelectionDialog() {
        MusicViewModel viewModel = new ViewModelProvider(requireActivity()).get(MusicViewModel.class);
        List<Song> songs = viewModel.getSongs().getValue();
        if (songs == null || songs.isEmpty()) {
            Toast.makeText(requireContext(), "Không có bài hát để sao lưu!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] songTitles = songs.stream().map(Song::getTitle).toArray(String[]::new);
        boolean[] checkedItems = new boolean[songs.size()];
        List<Song> selectedSongs = new ArrayList<>();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn bài hát để sao lưu")
                .setMultiChoiceItems(songTitles, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedSongs.add(songs.get(which));
                    } else {
                        selectedSongs.remove(songs.get(which));
                    }
                })
                .setPositiveButton("Sao lưu", (dialog, which) -> {
                    if (selectedSongs.isEmpty()) {
                        Toast.makeText(requireContext(), "Chưa chọn bài hát nào!", Toast.LENGTH_SHORT).show();
                    } else {
                        backupRestoreManager.backupSongs(selectedSongs, () -> notifySongFragment());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Đăng nhập thành công với Google", Toast.LENGTH_SHORT).show();
                        if (driveServiceHelper == null) {
                            initDriveServiceAsync();
                        }
                        updateUserInfo();
                    } else {
                        Toast.makeText(requireContext(), "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void logoutUser() {
        mAuth.signOut();
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(task -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(task2 -> {
                Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                driveServiceHelper = null;
                backupRestoreManager = null;
                isDriveInitFailed = false;
                updateUserInfo();
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });
    }

    private void updateUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userInfoLayout.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            backupMusicButton.setEnabled(true);
            restoreMusicButton.setEnabled(true);
            userName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Người dùng");
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this).load(currentUser.getPhotoUrl()).placeholder(R.drawable.baseline_account_circle_24).into(userPhoto);
            }
        } else {
            userInfoLayout.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            backupMusicButton.setEnabled(false);
            restoreMusicButton.setEnabled(false);
        }
    }

    private void scanSongsFromFolder(Uri treeUri) {
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        dbHelper.clearSongs(); // Xóa dữ liệu cũ trước khi quét folder mới
        songScanner.scanFolderAsync(treeUri, songs -> {
            if (isAdded() && !requireActivity().isFinishing()) {
                requireActivity().runOnUiThread(() -> {
                    uiHelper.showCustomToast("Đã quét xong " + songs.size() + " bài hát");
                    notifySongFragment();
                });
            }
        });
    }

    private void setupAutoScan() {
        if (scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }

        int intervalMinutes = prefs.getInt(KEY_SCAN_INTERVAL, 0);
        if (intervalMinutes > 0) {
            long intervalMillis = intervalMinutes * 60 * 1000L;
            scanRunnable = new Runnable() {
                @Override
                public void run() {
                    Uri treeUri = songScanner.loadTreeUri();
                    if (treeUri != null && songScanner.hasUriPermission(treeUri)) {
                        songScanner.scanFolderAsync(treeUri, songs -> notifySongFragment());
                    } else {
                        Log.w("SettingFragment", "No valid treeUri for auto-scan, skipping...");
                    }
                    scanHandler.postDelayed(this, intervalMillis);
                }
            };
            scanHandler.postDelayed(scanRunnable, intervalMillis);
        }
    }

    private void notifySongFragment() {
        for (Fragment fragment : getParentFragmentManager().getFragments()) {
            if (fragment instanceof SongFragment && fragment.isVisible()) {
                ((SongFragment) fragment).refreshSongsInBackground();
                break;
            }
        }
    }

    private void setSwitchState(int mode, boolean auto) {
        autoSwitch.setChecked(auto);
        darkSwitch.setChecked(mode == AppCompatDelegate.MODE_NIGHT_YES);
        darkSwitch.setEnabled(!auto);
    }

    private void saveAndApplyTheme(int mode, boolean auto) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_MODE, mode);
        editor.putBoolean(KEY_AUTO, auto);
        editor.apply();
        applyTheme(mode, auto);
        requireActivity().recreate();
    }

    private void applyTheme(int mode, boolean auto) {
        if (auto) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scanRunnable != null) {
            scanHandler.removeCallbacks(scanRunnable);
        }
    }

    @Override
    public void onSelectFolder() {}

    @Override
    public void onAutoScanIntervalChanged(int intervalMinutes) {
        prefs.edit().putInt(KEY_SCAN_INTERVAL, intervalMinutes).apply();
        setupAutoScan();
    }
}