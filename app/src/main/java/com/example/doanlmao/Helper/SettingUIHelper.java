package com.example.doanlmao.Helper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;

import com.example.doanlmao.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class SettingUIHelper {

    private Context context;
    private final SettingFragmentCallbacks callbacks;

    public interface SettingFragmentCallbacks {
        void onSelectFolder();
        void onAutoScanIntervalChanged(int intervalMinutes);
    }

    public SettingUIHelper(Context context, SettingFragmentCallbacks callbacks) {
        this.context = context;
        this.callbacks = callbacks;
    }

    public void showAutoScanDialog(int currentInterval, Runnable onSave) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_auto_scan, null);
        builder.setView(dialogView);

        SeekBar seekBar = dialogView.findViewById(R.id.seek_bar_auto_scan);
        TextView intervalText = dialogView.findViewById(R.id.interval_text);
        MaterialButton saveButton = dialogView.findViewById(R.id.save_button);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancel_button);

        int[] intervals = {0, 15, 30, 60, 120, 300};
        String[] intervalLabels = {"Tắt", "15 phút", "30 phút", "1 giờ", "2 giờ", "5 giờ"};
        int defaultProgress = 2;
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] == currentInterval) {
                defaultProgress = i;
                break;
            }
        }

        seekBar.setMax(intervals.length - 1);
        seekBar.setProgress(defaultProgress);
        intervalText.setText(intervalLabels[defaultProgress]);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intervalText.setText(intervalLabels[progress]);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            int selectedInterval = intervals[seekBar.getProgress()];
            callbacks.onAutoScanIntervalChanged(selectedInterval);
            Toast.makeText(context, "Đã cài đặt tự động quét: " + intervalLabels[seekBar.getProgress()], Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            onSave.run();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public void showCustomToast(String message) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View toastView = inflater.inflate(android.R.layout.simple_list_item_1, null);
        TextView textView = toastView.findViewById(android.R.id.text1);
        textView.setText(message);
        textView.setGravity(Gravity.CENTER);

        Toast toast = new Toast(context);
        toast.setView(toastView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
}
