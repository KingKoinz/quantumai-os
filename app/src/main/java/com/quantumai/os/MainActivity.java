package com.quantumai.os;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Create WebView
            WebView webView = new WebView(this);
            
            // Enable JavaScript
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            
            // Set WebView client
            webView.setWebViewClient(new WebViewClient());
            
            // Load URL
            webView.loadUrl("http://127.0.0.1:5000");
            
            // Set as content view
            setContentView(webView);
            
            Toast.makeText(this, "QuantumAI OS Loading...", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }
}
