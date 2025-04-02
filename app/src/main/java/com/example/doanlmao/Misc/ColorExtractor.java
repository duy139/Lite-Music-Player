package com.example.doanlmao.Misc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;

import androidx.palette.graphics.Palette;

public class ColorExtractor {
    private static final int SMALL_SIZE = 10; // Giảm kích thước ảnh để tăng tốc độ
    private static final float BLUR_RADIUS = 5f; // Giảm độ mờ để tăng tốc độ

    private final Context context;
    private final View targetView;
    private final OnColorExtractedListener listener;
    private Bitmap cachedBitmap; // Lưu bitmap để tái sử dụng

    public interface OnColorExtractedListener {
        void onColorExtracted(int dominantColor, int textColor);
    }

    public ColorExtractor(Context context, View targetView, OnColorExtractedListener listener) {
        this.context = context;
        this.targetView = targetView;
        this.listener = listener;
        setDefaultBackgroundAndColors(); // Đặt màu mặc định ngay từ đầu
    }

    public void extractColors(Bitmap bitmap) {
        if (bitmap == null) {
            setDefaultBackgroundAndColors();
            return;
        }

        // Lưu bitmap để tái sử dụng
        this.cachedBitmap = bitmap;

        // Chuyển xử lý ảnh sang luồng nền
        new Thread(() -> {
            // Resize ảnh
            Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, SMALL_SIZE, SMALL_SIZE, true);

            // Trích xuất màu chủ đạo
            Palette palette = Palette.from(smallBitmap).generate();
            Palette.Swatch dominantSwatch = palette != null ? palette.getDominantSwatch() : null;
            int dominantColor = dominantSwatch != null ? lightenColor(dominantSwatch.getRgb(), 0.3f) : Color.WHITE;

            int backgroundColor = getBackgroundColorAtBottom(smallBitmap);
            int textColor = getContrastColor(backgroundColor);

            // Cập nhật UI trên luồng chính
            new Handler(Looper.getMainLooper()).post(() -> {
                listener.onColorExtracted(dominantColor, textColor);
            });

            // Làm mờ ảnh (tùy chọn, có thể bỏ nếu không cần thiết)
            Bitmap blurredBitmap = blurBitmap(smallBitmap);
            updateBackground(blurredBitmap);
        }).start();
    }

    private void setDefaultBackgroundAndColors() {
        GradientDrawable defaultGradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        0x00000000,
                        0x801B1A1F,
                        0x401B1A1F,
                        0xFF000000
                }
        );
        targetView.setBackground(defaultGradient);
        listener.onColorExtracted(Color.WHITE, Color.WHITE);
    }

    private void updateBackground(Bitmap blurredBitmap) {
        new Handler(Looper.getMainLooper()).post(() -> {
            int screenWidth = targetView.getWidth();
            int screenHeight = targetView.getHeight();
            if (screenWidth == 0 || screenHeight == 0) {
                screenWidth = context.getResources().getDisplayMetrics().widthPixels;
                screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            }

            int blurredWidth = blurredBitmap.getWidth();
            int blurredHeight = blurredBitmap.getHeight();
            float scaleFactor = Math.max((float) screenWidth / blurredWidth, (float) screenHeight / blurredHeight);
            int newWidth = Math.round(blurredWidth * scaleFactor);
            int newHeight = Math.round(blurredHeight * scaleFactor);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(blurredBitmap, newWidth, newHeight, true);
            int cropX = (newWidth - screenWidth) / 2;
            int cropY = (newHeight - screenHeight) / 2;
            Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, cropX, cropY, screenWidth, screenHeight);

            BitmapDrawable blurredBackground = new BitmapDrawable(context.getResources(), croppedBitmap);
            GradientDrawable gradientOverlay = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            0x00000000,
                            0x40000000,
                            0x80000000,
                            0xFF000000
                    }
            );

            Drawable[] layers = new Drawable[]{blurredBackground, gradientOverlay};
            LayerDrawable layerDrawable = new LayerDrawable(layers);
            targetView.setBackground(layerDrawable);
        });
    }

    private Bitmap blurBitmap(Bitmap source) {
        Bitmap outputBitmap = Bitmap.createBitmap(source);
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        Allocation tmpIn = Allocation.createFromBitmap(rs, source);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

        blurScript.setRadius(BLUR_RADIUS);
        blurScript.setInput(tmpIn);
        blurScript.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        tmpIn.destroy();
        tmpOut.destroy();
        blurScript.destroy();
        rs.destroy();

        return outputBitmap;
    }

    private int getBackgroundColorAtBottom(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bottomAreaHeight = height / 4;
        long red = 0, green = 0, blue = 0;
        int pixelCount = 0;

        for (int x = 0; x < width; x++) {
            for (int y = height - bottomAreaHeight; y < height; y++) {
                int pixel = bitmap.getPixel(x, y);
                red += Color.red(pixel);
                green += Color.green(pixel);
                blue += Color.blue(pixel);
                pixelCount++;
            }
        }

        return Color.rgb(
                (int) (red / pixelCount),
                (int) (green / pixelCount),
                (int) (blue / pixelCount)
        );
    }

    private int lightenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.min(1.0f, hsv[2] + factor);
        return Color.HSVToColor(hsv);
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
}