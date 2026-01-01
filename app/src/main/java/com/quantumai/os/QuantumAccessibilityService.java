package com.quantumai.os;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class QuantumAccessibilityService extends AccessibilityService {

    private BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            String params = intent.getStringExtra("params");

            if (action != null) {
                executeAction(action, params);
            }
        }
    };

    @Override
    public void onServiceConnected() {
        // Register broadcast receiver for commands from WebUI
        IntentFilter filter = new IntentFilter("com.quantumai.ACCESSIBILITY_ACTION");
        // Fix for Android 13+: Specify receiver export flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(commandReceiver, filter);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Can be used for recording workflows in the future
    }

    @Override
    public void onInterrupt() {
        // Required override
    }

    private void executeAction(String action, String params) {
        switch (action) {
            case "tap":
                executeTap(params);
                break;

            case "swipe":
                executeSwipe(params);
                break;

            case "back":
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;

            case "home":
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;

            case "recents":
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;

            case "notifications":
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                break;

            case "quick_settings":
                performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
                break;
        }
    }

    private void executeTap(String params) {
        if (params == null) return;

        try {
            // Parse coordinates: "x,y"
            String[] coords = params.split(",");
            if (coords.length != 2) return;

            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());

            // Create tap gesture
            Path path = new Path();
            path.moveTo(x, y);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, 50);
            builder.addStroke(stroke);

            // Dispatch gesture
            dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    sendResult("tap", true, "Tapped at " + x + "," + y);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    sendResult("tap", false, "Tap cancelled");
                }
            }, null);

        } catch (Exception e) {
            sendResult("tap", false, "Error: " + e.getMessage());
        }
    }

    private void executeSwipe(String params) {
        if (params == null) return;

        try {
            // Parse swipe: "x1,y1,x2,y2,duration"
            String[] parts = params.split(",");
            if (parts.length < 4) return;

            int x1 = Integer.parseInt(parts[0].trim());
            int y1 = Integer.parseInt(parts[1].trim());
            int x2 = Integer.parseInt(parts[2].trim());
            int y2 = Integer.parseInt(parts[3].trim());
            int duration = parts.length > 4 ? Integer.parseInt(parts[4].trim()) : 300;

            // Create swipe gesture
            Path path = new Path();
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);

            GestureDescription.Builder builder = new GestureDescription.Builder();
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, duration);
            builder.addStroke(stroke);

            // Dispatch gesture
            dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    sendResult("swipe", true, "Swiped from " + x1 + "," + y1 + " to " + x2 + "," + y2);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    sendResult("swipe", false, "Swipe cancelled");
                }
            }, null);

        } catch (Exception e) {
            sendResult("swipe", false, "Error: " + e.getMessage());
        }
    }

    private void sendResult(String action, boolean success, String message) {
        Intent intent = new Intent("com.quantumai.ACCESSIBILITY_RESULT");
        intent.putExtra("action", action);
        intent.putExtra("success", success);
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(commandReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
    }
}
