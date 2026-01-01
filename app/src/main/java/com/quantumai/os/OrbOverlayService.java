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
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.core.app.NotificationCompat;

public class OrbOverlayService extends Service {

    private static final String CHANNEL_ID = "QuantumAI_Orb";
    private static final int NOTIFICATION_ID = 1001;
    private static final String ACTION_SHOW_ORB = "com.quantumai.ACTION_SHOW_ORB";

    private WindowManager windowManager;
    private FrameLayout orbView;
    private View orbCircle;
    private WindowManager.LayoutParams orbLayoutParams;

    private String currentState = "idle";
    private boolean isPulsing = false;
    private boolean isOrbVisible = true;

    // Color scheme - 50% transparency
    private static final int COLOR_IDLE = Color.parseColor("#804A90E2");
    private static final int COLOR_LISTENING = Color.parseColor("#8050E3C2");
    private static final int COLOR_THINKING = Color.parseColor("#80F5A623");
    private static final int COLOR_SPEAKING = Color.parseColor("#807ED321");
    private static final int COLOR_ERROR = Color.parseColor("#80D0021B");

    private BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_SHOW_ORB.equals(action)) {
                showOrb();
            } else {
                String state = intent.getStringExtra("state");
                if (state != null) {
                    updateOrbState(state);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.quantumai.ORB_STATE");
        filter.addAction(ACTION_SHOW_ORB);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            createOrbOverlay();
        }

        startForegroundServiceCompat();
    }

    private void startForegroundServiceCompat() {
        Notification notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createOrbOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        orbView = new FrameLayout(this);
        orbCircle = new View(this);
        
        int orbSize = (int) (30 * getResources().getDisplayMetrics().density); 
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(orbSize, orbSize);
        params.gravity = Gravity.CENTER;
        orbCircle.setLayoutParams(params);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(COLOR_IDLE);
        orbCircle.setBackground(background);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            orbCircle.setElevation(8);
        }

        orbView.addView(orbCircle);

        orbLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        orbLayoutParams.gravity = Gravity.TOP | Gravity.END;
        orbLayoutParams.x = 20;
        orbLayoutParams.y = 200;

        setupGestureDetection(orbView);

        try {
            windowManager.addView(orbView, orbLayoutParams);
            isOrbVisible = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateOrbState("idle");
    }

    private void setupGestureDetection(View view) {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                hideOrb();
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                handleOrbTap();
                return true;
            }
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = orbLayoutParams.x;
                        initialY = orbLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        orbLayoutParams.x = initialX + (int) (initialTouchX - event.getRawX());
                        orbLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        try {
                            windowManager.updateViewLayout(orbView, orbLayoutParams);
                        } catch (Exception ex) {}
                        return true;
                }
                return false;
            }
        });
    }

    private void hideOrb() {
        if (isOrbVisible && orbView != null) {
            orbView.setVisibility(View.GONE);
            isOrbVisible = false;
            // Update notification to show "Tap to restore"
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    private void showOrb() {
        if (!isOrbVisible && orbView != null) {
            orbView.setVisibility(View.VISIBLE);
            isOrbVisible = true;
            // Restore normal notification
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    private void handleOrbTap() {
        Intent intent = new Intent("com.quantumai.ORB_TAPPED");
        sendBroadcast(intent);
    }

    private void updateOrbState(String state) {
        if (state.equals(currentState)) return;
        currentState = state;
        isPulsing = false;
        orbCircle.animate().cancel();

        switch (state) {
            case "idle":
                setOrbColor(COLOR_IDLE);
                startPulse(1000, 1.15f);
                break;
            case "listening":
                setOrbColor(COLOR_LISTENING);
                orbCircle.setAlpha(0.6f);
                break;
            case "thinking":
                setOrbColor(COLOR_THINKING);
                startPulse(1200, 1.1f);
                break;
            case "speaking":
                setOrbColor(COLOR_SPEAKING);
                startPulse(400, 1.2f);
                break;
            case "error":
                setOrbColor(COLOR_ERROR);
                orbCircle.setAlpha(0.6f);
                break;
        }
    }

    private void setOrbColor(int color) {
        if (orbCircle.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) orbCircle.getBackground()).setColor(color);
        }
    }

    private void startPulse(int duration, float scale) {
        isPulsing = true;
        pulseAnimation(duration, scale);
    }

    private void pulseAnimation(int duration, float scale) {
        if (!isPulsing) return;
        orbCircle.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(0.4f)
                .setDuration(duration)
                .withEndAction(() -> {
                    orbCircle.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(0.6f)
                            .setDuration(duration)
                            .withEndAction(() -> {
                                if (isPulsing) pulseAnimation(duration, scale);
                            });
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "QuantumAI Orb", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        // Intent to restore the orb when notification is tapped
        Intent showIntent = new Intent(this, OrbOverlayService.class);
        showIntent.setAction(ACTION_SHOW_ORB);
        PendingIntent pendingIntent = PendingIntent.getService(
                this, 0, showIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String contentText = isOrbVisible ? "Orb active (Double tap orb to hide)" : "Orb hidden (Tap here to restore)";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("QuantumAI OS")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SHOW_ORB.equals(intent.getAction())) {
            showOrb();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isPulsing = false;
        if (orbView != null && windowManager != null) {
            try { windowManager.removeView(orbView); } catch (Exception e) {}
        }
        try { unregisterReceiver(stateReceiver); } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
