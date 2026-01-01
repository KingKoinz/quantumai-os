package com.quantumai.os;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Check and request overlay permission for orb
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Please grant overlay permission for orb", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                } else {
                    startOrbService();
                }
            } else {
                startOrbService();
            }

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
            
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void startOrbService() {
        try {
            Intent intent = new Intent(this, OrbOverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not start orb: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permission granted! Starting orb...", Toast.LENGTH_SHORT).show();
                    startOrbService();
                } else {
                    Toast.makeText(this, "Overlay permission denied - orb disabled", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
```

**4. Commit changes** → Message: "Add orb overlay service"

---

## **What This Does:**

✅ Keeps WebView working  
✅ Asks for overlay permission when you open app  
✅ Starts orb service when permission granted  
✅ Shows helpful messages  
✅ Won't crash if orb fails  

---

## **After New Build Completes:**

**1. Download new APK** (same process as before)

**2. Install it** (will update existing app)

**3. When you open app:**
- It will ask for overlay permission
- Grant it
- **Blue orb should appear!** ✨

**4. Grant accessibility too** (if you want tap/swipe automation):
```
Settings → Accessibility → QuantumAI OS → Toggle ON
