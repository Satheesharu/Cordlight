package com.example.pullcordtorch;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int CAMERA_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 48, 32, 32);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Pull Cord Torch");
        title.setTextSize(26);
        title.setPadding(0, 0, 0, 18);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("Enable the permissions, then show the hanging pull-cord on top of your other apps. Drag the handle down to toggle the flashlight.");
        info.setTextSize(16);
        info.setPadding(0, 0, 0, 24);
        layout.addView(info);

        Button start = new Button(this);
        start.setText("Enable pull-cord");
        start.setOnClickListener(v -> prepareAndStart());
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("Hide pull-cord");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, PullCordService.class));
            Toast.makeText(this, "Pull-cord hidden", Toast.LENGTH_SHORT).show();
        });
        layout.addView(stop);

        setContentView(layout);
    }

    private void prepareAndStart() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(overlayIntent);
            Toast.makeText(this, "Allow display over other apps, then return here and tap Enable pull-cord.", Toast.LENGTH_LONG).show();
            return;
        }
        startPullCordService();
    }

    private void startPullCordService() {
        Intent serviceIntent = new Intent(this, PullCordService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "Pull-cord enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            prepareAndStart();
        } else if (requestCode == CAMERA_REQUEST) {
            Toast.makeText(this, "Camera permission is needed to control the flashlight.", Toast.LENGTH_LONG).show();
        }
    }
}
