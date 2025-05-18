package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 101;
    
    private Button startStopButton;
    private TextView statusText;
    private boolean isServiceRunning = false;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE);
        isServiceRunning = sharedPreferences.getBoolean("isServiceRunning", false);

        statusText = findViewById(R.id.status_text);
        startStopButton = findViewById(R.id.start_stop_button);
        
        updateUI();
        
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning) {
                    stopService();
                } else {
                    checkAndRequestPermissions();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        if (isServiceRunning) {
            statusText.setText(R.string.service_running);
            startStopButton.setText(R.string.stop_service);
        } else {
            statusText.setText(R.string.service_stopped);
            startStopButton.setText(R.string.start_service);
        }
    }

    private void checkAndRequestPermissions() {
        // Check if accessibility service is enabled
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Erişilebilirlik servisini etkinleştirmeniz gerekiyor", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            return;
        }

        // Check overlay permission for Android M and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Diğer uygulamaların üzerinde görüntüleme izni gerekiyor", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // All permissions granted, start the service
        startService();
    }

    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getPackageName() + "/" + AutoClickerAccessibilityService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        return enabledServices != null && enabledServices.contains(serviceName);
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        isServiceRunning = true;
        sharedPreferences.edit().putBoolean("isServiceRunning", true).apply();
        updateUI();
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
        sharedPreferences.edit().putBoolean("isServiceRunning", false).apply();
        updateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Permission granted, check accessibility
                    if (isAccessibilityServiceEnabled()) {
                        startService();
                    } else {
                        Toast.makeText(this, "Erişilebilirlik servisini etkinleştirmeniz gerekiyor", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(this, "İzin verilmedi, uygulama çalışmayacak", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
} 