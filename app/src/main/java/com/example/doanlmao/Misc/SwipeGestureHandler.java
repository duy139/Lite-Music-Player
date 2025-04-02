package com.example.doanlmao.Misc;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SwipeGestureHandler {
    private static final float SWIPE_THRESHOLD = 100f;
    private static final float SWIPE_VELOCITY_THRESHOLD = 200f;
    private static final float ESCAPE_VELOCITY_THRESHOLD = 1000f;
    private static final int FADE_DURATION = 100;
    private static final long DEBOUNCE_TIME = 500;
    private static final float SLIDE_DOWN_DISTANCE = 200f; // Giảm từ 300dp xuống 200dp
    private static final float ESCAPE_THRESHOLD_PERCENT = 0.2f;
    private static final float DRAG_DAMPING_FACTOR = 0.8f;

    private final View swipeArea;
    private final ImageView coverArt;
    private final TextView songName;
    private final TextView songArtist;
    private final TextView songAlbum;
    private final View rootView;
    private final OnSwipeListener listener;

    private float startX, startY;
    private boolean isSwiping = false;
    private boolean isDragging = false;
    private boolean isVerticalSwipe = false;
    private long lastSwipeTime = 0;
    private float screenHeight;

    private static final String TAG = "SwipeGestureHandler";

    public interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
        void onSwipeDown();
    }

    public SwipeGestureHandler(View swipeArea, ImageView coverArt, TextView songName, TextView songArtist, TextView songAlbum, View rootView, OnSwipeListener listener) {
        this.swipeArea = swipeArea;
        this.coverArt = coverArt;
        this.songName = songName;
        this.songArtist = songArtist;
        this.songAlbum = songAlbum;
        this.rootView = rootView;
        this.listener = listener;

        DisplayMetrics displayMetrics = swipeArea.getResources().getDisplayMetrics();
        screenHeight = displayMetrics.heightPixels;

        if (swipeArea != null) {
            setupSwipeListener();
        } else {
            Log.e(TAG, "SwipeArea null, đm không setup được listener!");
        }
    }

    private void setupSwipeListener() {
        swipeArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        isDragging = false;
                        isVerticalSwipe = false;
                        rootView.animate().cancel();
                        resetView();
                        Log.d(TAG, "ACTION_DOWN: startX = " + startX + ", startY = " + startY);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float currentY = event.getY();
                        float currentX = event.getX();
                        float moveDeltaY = currentY - startY;
                        float moveDeltaX = currentX - startX;

                        if (!isDragging && !isVerticalSwipe) {
                            if (Math.abs(moveDeltaY) > Math.abs(moveDeltaX) && moveDeltaY > 0) {
                                isVerticalSwipe = true;
                                isDragging = true;
                                Log.d(TAG, "Vertical swipe detected");
                            }
                        }

                        if (isVerticalSwipe) {
                            float adjustedDeltaY = moveDeltaY * DRAG_DAMPING_FACTOR;
                            rootView.setTranslationY(adjustedDeltaY);

                            float maxDistance = screenHeight * ESCAPE_THRESHOLD_PERCENT;
                            float alpha = 1f - Math.min(moveDeltaY / maxDistance, 1f);
                            rootView.setAlpha(Math.max(alpha, 0f));
                            Log.d(TAG, "ACTION_MOVE: moveDeltaY = " + moveDeltaY + ", adjustedDeltaY = " + adjustedDeltaY + ", alpha = " + alpha);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastSwipeTime < DEBOUNCE_TIME) {
                            Log.d(TAG, "Debounce: Ignoring swipe");
                            resetView();
                            return true;
                        }

                        if (isSwiping) {
                            Log.d(TAG, "Ignoring swipe: Animation in progress");
                            resetView();
                            return true;
                        }

                        float endX = event.getX();
                        float endY = event.getY();
                        float deltaX = endX - startX;
                        float deltaY = endY - startY;
                        float velocityX = Math.abs(deltaX) / (event.getEventTime() - event.getDownTime()) * 1000;
                        float velocityY = Math.abs(deltaY) / (event.getEventTime() - event.getDownTime()) * 1000;

                        if (!isVerticalSwipe && Math.abs(deltaX) > Math.abs(deltaY) && (Math.abs(deltaX) > SWIPE_THRESHOLD || velocityX > SWIPE_VELOCITY_THRESHOLD)) {
                            isSwiping = true;
                            lastSwipeTime = currentTime;
                            if (deltaX < 0) {
                                Log.d(TAG, "Swipe Left detected");
                                fadeOutAndSwipe(() -> listener.onSwipeLeft());
                            } else {
                                Log.d(TAG, "Swipe Right detected");
                                fadeOutAndSwipe(() -> listener.onSwipeRight());
                            }
                        } else if (isVerticalSwipe || (deltaY > SWIPE_THRESHOLD || velocityY > SWIPE_VELOCITY_THRESHOLD)) {
                            isSwiping = true;
                            lastSwipeTime = currentTime;
                            float escapeThreshold = screenHeight * ESCAPE_THRESHOLD_PERCENT;
                            if (velocityY > ESCAPE_VELOCITY_THRESHOLD || deltaY >= escapeThreshold) {
                                Log.d(TAG, "Swipe Down detected - Escaping, velocityY = " + velocityY);
                                animateSlideDownAndFadeOut(() -> listener.onSwipeDown());
                            } else {
                                Log.d(TAG, "Swipe Down not enough - Returning, velocityY = " + velocityY);
                                animateReturnToOriginal();
                            }
                        } else {
                            resetView();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void fadeOutAndSwipe(Runnable onSwipeAction) {
        coverArt.animate().alpha(0f).setDuration(FADE_DURATION).start();
        songName.animate().alpha(0f).setDuration(FADE_DURATION).start();
        songArtist.animate().alpha(0f).setDuration(FADE_DURATION).start();
        songAlbum.animate().alpha(0f).setDuration(FADE_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        onSwipeAction.run();
                        fadeIn();
                    }
                }).start();
    }

    private void fadeIn() {
        coverArt.animate().alpha(1f).setDuration(FADE_DURATION).start();
        songName.animate().alpha(1f).setDuration(FADE_DURATION).start();
        songArtist.animate().alpha(1f).setDuration(FADE_DURATION).start();
        songAlbum.animate().alpha(1f).setDuration(FADE_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isSwiping = false;
                        Log.d(TAG, "Fade in completed");
                    }
                }).start();
    }

    private void animateSlideDownAndFadeOut(Runnable onSwipeAction) {
        float slideDistancePx = SLIDE_DOWN_DISTANCE * swipeArea.getResources().getDisplayMetrics().density;

        rootView.animate()
                .alpha(0f)
                .translationY(slideDistancePx)
                .setDuration(FADE_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        onSwipeAction.run();
                        isSwiping = false;
                    }
                })
                .start();
    }

    private void animateReturnToOriginal() {
        rootView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(FADE_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isSwiping = false;
                        Log.d(TAG, "Returned to original position");
                    }
                })
                .start();
    }

    private void resetView() {
        rootView.setAlpha(1f);
        rootView.setTranslationY(0f);
        isDragging = false;
        isVerticalSwipe = false;
    }
}