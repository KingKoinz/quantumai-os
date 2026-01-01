package com.quantumai.os;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_AUDIO_PERMISSION = 1002;
    private static final int REQUEST_VOICE_INPUT = 1003;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check and request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            startOrbService();
        }

        // Check audio permission for voice input
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO_PERMISSION);
        }

        // Setup WebView
        setupWebView();
    }

    private void setupWebView() {
        webView = new WebView(this);

        // Enable JavaScript
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Add JavaScript interface for Android capabilities
        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        // WebView clients
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // Load QuantumAI WebUI
        webView.loadUrl("http://127.0.0.1:5000");

        setContentView(webView);
    }

    private void startOrbService() {
        Intent intent = new Intent(this, OrbOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startOrbService();
            } else {
                Toast.makeText(this, "Overlay permission required for orb", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_VOICE_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String voiceText = results.get(0);
                // Send voice input to WebUI
                webView.evaluateJavascript(
                        "if (window.handleVoiceInput) { window.handleVoiceInput('" +
                                voiceText.replace("'", "\\'") + "'); }",
                        null
                );
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * JavaScript Bridge - Exposes Android capabilities to WebUI
     */
    public class AndroidBridge {

        @JavascriptInterface
        public void setOrbState(String state) {
            // Broadcast orb state change to OrbOverlayService
            Intent intent = new Intent("com.quantumai.ORB_STATE");
            intent.putExtra("state", state);
            sendBroadcast(intent);
        }

        @JavascriptInterface
        public void executeAccessibilityAction(String action, String params) {
            // Broadcast accessibility command to QuantumAccessibilityService
            Intent intent = new Intent("com.quantumai.ACCESSIBILITY_ACTION");
            intent.putExtra("action", action);
            intent.putExtra("params", params);
            sendBroadcast(intent);
        }

        @JavascriptInterface
        public void startVoiceInput() {
            // Launch voice recognition
            runOnUiThread(() -> {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to QuantumAI");
                startActivityForResult(intent, REQUEST_VOICE_INPUT);
            });
        }

        @JavascriptInterface
        public String getDeviceInfo() {
            return Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")";
        }

        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }
}
