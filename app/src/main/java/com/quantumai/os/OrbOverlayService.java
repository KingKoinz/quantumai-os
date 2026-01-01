package com.quantumai.os;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.core.app.NotificationCompat;

public class OrbOverlayService extends Service {

    private static final String CHANNEL_ID = "QuantumAI_Orb";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private FrameLayout orbView;
    private View orbCircle;

    private String currentState = "idle";
    private boolean isPulsing = false;

    // Color scheme
    private static final int COLOR_IDLE = Color.parseColor("#4A90E2");      // Blue
    private static final int COLOR_LISTENING = Color.parseColor("#50E3C2"); // Cyan
    private static final int COLOR_THINKING = Color.parseColor("#F5A623");  // Orange
    private static final int COLOR_SPEAKING = Color.parseColor("#7ED321");  // Green
    private static final int COLOR_ERROR = Color.parseColor("#D0021B");     // Red

    private BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra("state");
            if (state != null) {
                updateOrbState(state);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel
        createNotificationChannel();

        // Register broadcast receiver for state updates
        IntentFilter filter = new IntentFilter("com.quantumai.ORB_STATE");
        registerReceiver(stateReceiver, filter);

        // Create orb overlay
        createOrbOverlay();

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
    }

    private void createOrbOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Create orb container
        orbView = new FrameLayout(this);

        // Create orb circle
        orbCircle = new View(this);
        int orbSize = 120; // dp
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(orbSize, orbSize);
        params.gravity = Gravity.CENTER;
        orbCircle.setLayoutParams(params);

        // Set initial appearance
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(COLOR_IDLE);
        orbCircle.setBackground(background);
        orbCircle.setElevation(16);

        orbView.addView(orbCircle);

        // Window layout params
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        layoutParams.gravity = Gravity.TOP | Gravity.END;
        layoutParams.x = 20;
        layoutParams.y = 200;

        // Make orb draggable
        setupDraggable(orbView, layoutParams);

        // Make orb tappable
        orbView.setOnClickListener(v -> handleOrbTap());

        // Add to window
        windowManager.addView(orbView, layoutParams);

        // Start idle pulse
        updateOrbState("idle");
    }

    private void setupDraggable(View view, WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (initialTouchX - event.getRawX());
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(view, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Detect tap vs drag
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void handleOrbTap() {
        // Notify MainActivity to start voice input
        Intent intent = new Intent("com.quantumai.ORB_TAPPED");
        sendBroadcast(intent);

        // Could also directly trigger voice here
        // For now, let MainActivity handle it via WebUI
    }

    private void updateOrbState(String state) {
        if (state.equals(currentState)) return;

        currentState = state;
        isPulsing = false;
        orbCircle.clearAnimation();

        switch (state) {
            case "idle":
                setOrbColor(COLOR_IDLE);
                startPulse(1000, 1.15f); // Gentle pulse
                break;

            case "listening":
                setOrbColor(COLOR_LISTENING);
                orbCircle.setAlpha(1.0f);
                orbCircle.setScaleX(1.0f);
                orbCircle.setScaleY(1.0f);
                break;

            case "thinking":
                setOrbColor(COLOR_THINKING);
                startPulse(1200, 1.1f); // Slow pulse
                break;

            case "speaking":
                setOrbColor(COLOR_SPEAKING);
                startPulse(400, 1.2f); // Fast pulse
                break;

            case "error":
                setOrbColor(COLOR_ERROR);
                orbCircle.setAlpha(1.0f);
                orbCircle.setScaleX(1.0f);
                orbCircle.setScaleY(1.0f);
                break;
        }
    }

    private void setOrbColor(int color) {
        GradientDrawable background = (GradientDrawable) orbCircle.getBackground();
        background.setColor(color);
    }

    private void startPulse(int duration, float scale) {
        isPulsing = true;
        pulseAnimation(duration, scale);
    }

    private void pulseAnimation(int duration, float scale) {
        orbCircle.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(0.7f)
                .setDuration(duration)
                .withEndAction(() -> {
                    orbCircle.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(duration)
                            .withEndAction(() -> {
                                if (isPulsing) {
                                    pulseAnimation(duration, scale);
                                }
                            });
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "QuantumAI Orb",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows QuantumAI status orb");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("QuantumAI OS")
                .setContentText("Orb active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (orbView != null && windowManager != null) {
            windowManager.removeView(orbView);
        }
        unregisterReceiver(stateReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
