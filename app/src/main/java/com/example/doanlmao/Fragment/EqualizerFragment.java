package com.example.doanlmao.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.doanlmao.Activity.MainActivity;
import com.example.doanlmao.Misc.EqualizerPreset;
import com.example.doanlmao.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EqualizerFragment extends Fragment {
    private SeekBar bassSeekBar, midSeekBar, trebleSeekBar, balanceSeekBar, volumeSeekBar, reverbSeekBar;
    private TextView bassLevel, midLevel, trebleLevel, balanceLevel, volumeLevel, reverbLevel;
    private MaterialButton savePresetButton;
    private Spinner presetSpinner;
    private SharedPreferences prefs;
    private List<EqualizerPreset> presetList;
    private ArrayAdapter<String> presetAdapter;
    private static final String PREFS_NAME = "EqualizerPrefs";
    private static final String KEY_BASS = "bassLevel";
    private static final String KEY_MID = "midLevel";
    private static final String KEY_TREBLE = "trebleLevel";
    private static final String KEY_CUSTOM1_BASS = "custom1Bass";
    private static final String KEY_CUSTOM1_MID = "custom1Mid";
    private static final String KEY_CUSTOM1_TREBLE = "custom1Treble";
    private static final String KEY_CUSTOM2_BASS = "custom2Bass";
    private static final String KEY_CUSTOM2_MID = "custom2Mid";
    private static final String KEY_CUSTOM2_TREBLE = "custom2Treble";
    private static final String KEY_SELECTED_PRESET = "selectedPreset";
    private AudioManager audioManager;
    private VolumeObserver volumeObserver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_equalizer, container, false);

        bassSeekBar = view.findViewById(R.id.bass_seekbar);
        midSeekBar = view.findViewById(R.id.mid_seekbar);
        trebleSeekBar = view.findViewById(R.id.treble_seekbar);
        balanceSeekBar = view.findViewById(R.id.balance_seekbar);
        volumeSeekBar = view.findViewById(R.id.volume_seekbar);
        reverbSeekBar = view.findViewById(R.id.reverb_seekbar);
        bassLevel = view.findViewById(R.id.bass_level);
        midLevel = view.findViewById(R.id.mid_level);
        trebleLevel = view.findViewById(R.id.treble_level);
        balanceLevel = view.findViewById(R.id.balance_level);
        volumeLevel = view.findViewById(R.id.volume_level);
        reverbLevel = view.findViewById(R.id.reverb_level);
        savePresetButton = view.findViewById(R.id.save_preset_button);
        presetSpinner = view.findViewById(R.id.preset_spinner);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

        // Khởi tạo danh sách preset
        presetList = new ArrayList<>();
        presetList.add(new EqualizerPreset("Flat", (short) 0, (short) 0, (short) 0));
        presetList.add(new EqualizerPreset("Pop", (short) 300, (short) 600, (short) 300));
        presetList.add(new EqualizerPreset("Rock", (short) 600, (short) 0, (short) 600));
        presetList.add(new EqualizerPreset("Bass Boost", (short) 900, (short) 0, (short) 0));
        presetList.add(new EqualizerPreset("Treble Boost", (short) 0, (short) 0, (short) 900));
        presetList.add(new EqualizerPreset("Jazz", (short) 400, (short) 500, (short) 700)); // Nhẹ nhàng, ấm
        presetList.add(new EqualizerPreset("Classical", (short) 200, (short) 400, (short) 600)); // Trong trẻo, cân bằng
        presetList.add(new EqualizerPreset("Dance", (short) 700, (short) 300, (short) 500)); // Bass mạnh, treble vừa
        presetList.add(new EqualizerPreset("Hip Hop", (short) 800, (short) 200, (short) 300)); // Bass nặng, mid thấp
        presetList.add(new EqualizerPreset("Electronic", (short) 600, (short) 400, (short) 800)); // Bass và treble nổi bật
        presetList.add(new EqualizerPreset("Vocal Boost", (short) 100, (short) 800, (short) 400)); // Tăng mid cho giọng hát
        presetList.add(new EqualizerPreset("Custom 1", (short) prefs.getInt(KEY_CUSTOM1_BASS, 0),
                (short) prefs.getInt(KEY_CUSTOM1_MID, 0), (short) prefs.getInt(KEY_CUSTOM1_TREBLE, 0)));
        presetList.add(new EqualizerPreset("Custom 2", (short) prefs.getInt(KEY_CUSTOM2_BASS, 0),
                (short) prefs.getInt(KEY_CUSTOM2_MID, 0), (short) prefs.getInt(KEY_CUSTOM2_TREBLE, 0)));

        // Thiết lập Spinner
        List<String> presetNames = presetList.stream().map(EqualizerPreset::getName).collect(Collectors.toList());
        presetAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, presetNames);
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(presetAdapter);

        // Load preset đã chọn trước đó
        int selectedPresetIndex = prefs.getInt(KEY_SELECTED_PRESET, 0);
        presetSpinner.setSelection(selectedPresetIndex);

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                EqualizerPreset preset = presetList.get(position);
                applyPreset(preset);
                // Lưu preset đã chọn
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_SELECTED_PRESET, position);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (MainActivity.musicService != null && MainActivity.musicService.getAudioSessionId() != 0) {
            setupEqualizerControls();
        } else {
            disableControls();
            Toast.makeText(requireContext(), "Chưa phát nhạc, không thể chỉnh âm!", Toast.LENGTH_SHORT).show();
        }

        savePresetButton.setOnClickListener(v -> savePreset());

        // Đăng ký ContentObserver để theo dõi thay đổi âm lượng
        volumeObserver = new VolumeObserver(new Handler(Looper.getMainLooper()));
        requireContext().getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true, volumeObserver);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy đăng ký ContentObserver
        if (volumeObserver != null) {
            requireContext().getContentResolver().unregisterContentObserver(volumeObserver);
        }
    }

    private class VolumeObserver extends ContentObserver {
        public VolumeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // Lấy âm lượng hệ thống mới
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            float systemVolume = (float) currentVolume / maxVolume;

            // Cập nhật giá trị của SeekBar Volume
            MainActivity.musicService.setVolumeGain(systemVolume);
            volumeSeekBar.setProgress((int) (systemVolume * 100));
            volumeLevel.setText(String.format("%.0f%%", systemVolume * 100));
        }
    }

    private void setupEqualizerControls() {
        short minLevel = MainActivity.musicService.getEqualizer().getBandLevelRange()[0];
        short maxLevel = MainActivity.musicService.getEqualizer().getBandLevelRange()[1];
        int numBands = MainActivity.musicService.getEqualizer().getNumberOfBands();

        // Log giới hạn
        Log.d("EqualizerFragment", "Equalizer band level range: min=" + minLevel + ", max=" + maxLevel);
        Log.d("EqualizerFragment", "Number of bands: " + numBands);

        // Load preset từ SharedPreferences
        short savedBass = (short) prefs.getInt(KEY_BASS, 0);
        short savedMid = (short) prefs.getInt(KEY_MID, 0);
        short savedTreble = (short) prefs.getInt(KEY_TREBLE, 0);
        float savedBalance = MainActivity.musicService.getBalance();
        float savedVolume = MainActivity.musicService.getVolumeGain();
        int savedReverb = prefs.getInt("reverbLevel", 0);

        // Áp dụng preset
        MainActivity.musicService.getEqualizer().setBandLevel((short) 0, savedBass);
        MainActivity.musicService.getEqualizer().setBandLevel((short) (numBands / 2), savedMid);
        MainActivity.musicService.getEqualizer().setBandLevel((short) (numBands - 1), savedTreble);
        MainActivity.musicService.setReverbLevel(savedReverb);

        configureSeekBar(bassSeekBar, bassLevel, 0, minLevel, maxLevel, savedBass);
        configureSeekBar(midSeekBar, midLevel, numBands / 2, minLevel, maxLevel, savedMid);
        configureSeekBar(trebleSeekBar, trebleLevel, numBands - 1, minLevel, maxLevel, savedTreble);
        configureBalanceSeekBar(balanceSeekBar, balanceLevel, savedBalance);
        configureVolumeSeekBar(volumeSeekBar, volumeLevel, savedVolume);
        configureReverbSeekBar(reverbSeekBar, reverbLevel, savedReverb);
    }

    private void configureSeekBar(SeekBar seekBar, TextView levelText, int band, short minLevel, short maxLevel, short savedLevel) {
        // Ánh xạ giá trị từ minLevel..maxLevel thành 0..30
        int progress = (int) (((savedLevel - minLevel) / (float) (maxLevel - minLevel)) * 30);
        seekBar.setProgress(progress);
        levelText.setText(savedLevel / 100 + " dB");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Ánh xạ progress (0..30) thành giá trị từ minLevel..maxLevel
                float fraction = progress / 30f;
                short level = (short) (minLevel + fraction * (maxLevel - minLevel));
                MainActivity.musicService.getEqualizer().setBandLevel((short) band, level);
                levelText.setText(String.format("%.1f dB", level / 100f));

                // Lưu giá trị vào SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                if (band == 0) {
                    editor.putInt(KEY_BASS, level);
                } else if (band == MainActivity.musicService.getEqualizer().getNumberOfBands() / 2) {
                    editor.putInt(KEY_MID, level);
                } else {
                    editor.putInt(KEY_TREBLE, level);
                }
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void configureBalanceSeekBar(SeekBar seekBar, TextView levelText, float savedBalance) {
        // Ánh xạ giá trị từ -1..1 thành 0..200
        int progress = (int) ((savedBalance + 1) * 100);
        seekBar.setProgress(progress);
        levelText.setText(String.format("%.0f%%", savedBalance * 100));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Ánh xạ progress (0..200) thành giá trị từ -1..1
                float balance = (progress / 100f) - 1;
                MainActivity.musicService.setBalance(balance);
                levelText.setText(String.format("%.0f%%", balance * 100));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void configureVolumeSeekBar(SeekBar seekBar, TextView levelText, float savedVolume) {
        // Ánh xạ giá trị từ 0..1 thành 0..100
        int progress = (int) (savedVolume * 100);
        seekBar.setProgress(progress);
        levelText.setText(String.format("%.0f%%", savedVolume * 100));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Ánh xạ progress (0..100) thành giá trị từ 0..1
                float volume = progress / 100f;
                MainActivity.musicService.setVolumeGain(volume);
                // Cập nhật âm lượng hệ thống
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int newVolume = (int) (volume * maxVolume);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
                levelText.setText(String.format("%.0f%%", volume * 100));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void configureReverbSeekBar(SeekBar seekBar, TextView levelText, int savedLevel) {
        seekBar.setProgress(savedLevel);
        levelText.setText(String.format("%d%%", savedLevel));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int level = progress;
                MainActivity.musicService.setReverbLevel(level);
                levelText.setText(String.format("%d%%", level));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void applyPreset(EqualizerPreset preset) {
        short bass = preset.getBass();
        short mid = preset.getMid();
        short treble = preset.getTreble();

        int numBands = MainActivity.musicService.getEqualizer().getNumberOfBands();
        MainActivity.musicService.getEqualizer().setBandLevel((short) 0, bass);
        MainActivity.musicService.getEqualizer().setBandLevel((short) (numBands / 2), mid);
        MainActivity.musicService.getEqualizer().setBandLevel((short) (numBands - 1), treble);

        // Reset Balance và Reverb
        MainActivity.musicService.setBalance(0f);
        MainActivity.musicService.setReverbLevel(0);

        // Ánh xạ giá trị cho SeekBar
        short minLevel = MainActivity.musicService.getEqualizer().getBandLevelRange()[0];
        short maxLevel = MainActivity.musicService.getEqualizer().getBandLevelRange()[1];
        bassSeekBar.setProgress((int) (((bass - minLevel) / (float) (maxLevel - minLevel)) * 30));
        midSeekBar.setProgress((int) (((mid - minLevel) / (float) (maxLevel - minLevel)) * 30));
        trebleSeekBar.setProgress((int) (((treble - minLevel) / (float) (maxLevel - minLevel)) * 30));
        balanceSeekBar.setProgress(100); // Balance = 0
        reverbSeekBar.setProgress(0); // Reverb = 0

        bassLevel.setText(bass / 100 + " dB");
        midLevel.setText(mid / 100 + " dB");
        trebleLevel.setText(treble / 100 + " dB");
        balanceLevel.setText("0%");
        volumeLevel.setText(String.format("%.0f%%", MainActivity.musicService.getVolumeGain() * 100));
        reverbLevel.setText("0%");

        // Lưu preset hiện tại
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_BASS, bass);
        editor.putInt(KEY_MID, mid);
        editor.putInt(KEY_TREBLE, treble);
        editor.apply();
    }

    private void savePreset() {
        short bass = MainActivity.musicService.getEqualizer().getBandLevel((short) 0);
        short mid = MainActivity.musicService.getEqualizer().getBandLevel((short) (MainActivity.musicService.getEqualizer().getNumberOfBands() / 2));
        short treble = MainActivity.musicService.getEqualizer().getBandLevel((short) (MainActivity.musicService.getEqualizer().getNumberOfBands() - 1));

        int selectedPosition = presetSpinner.getSelectedItemPosition();
        String selectedPresetName = presetList.get(selectedPosition).getName();

        SharedPreferences.Editor editor = prefs.edit();
        if (selectedPresetName.equals("Custom 1")) {
            editor.putInt(KEY_CUSTOM1_BASS, bass);
            editor.putInt(KEY_CUSTOM1_MID, mid);
            editor.putInt(KEY_CUSTOM1_TREBLE, treble);
            presetList.set(selectedPosition, new EqualizerPreset("Custom 1", bass, mid, treble));
        } else if (selectedPresetName.equals("Custom 2")) {
            editor.putInt(KEY_CUSTOM2_BASS, bass);
            editor.putInt(KEY_CUSTOM2_MID, mid);
            editor.putInt(KEY_CUSTOM2_TREBLE, treble);
            presetList.set(selectedPosition, new EqualizerPreset("Custom 2", bass, mid, treble));
        } else {
            // Lưu preset hiện tại
            editor.putInt(KEY_BASS, bass);
            editor.putInt(KEY_MID, mid);
            editor.putInt(KEY_TREBLE, treble);
        }
        editor.apply();

        Toast.makeText(requireContext(), "Đã lưu preset: " + selectedPresetName, Toast.LENGTH_SHORT).show();
    }

    private void disableControls() {
        bassSeekBar.setEnabled(false);
        midSeekBar.setEnabled(false);
        trebleSeekBar.setEnabled(false);
        balanceSeekBar.setEnabled(false);
        volumeSeekBar.setEnabled(false);
        reverbSeekBar.setEnabled(false);
        bassLevel.setText("N/A");
        midLevel.setText("N/A");
        trebleLevel.setText("N/A");
        balanceLevel.setText("N/A");
        volumeLevel.setText("N/A");
        reverbLevel.setText("N/A");
        savePresetButton.setEnabled(false);
        presetSpinner.setEnabled(false);
    }
}