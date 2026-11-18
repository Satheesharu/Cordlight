package com.example.pullcordtorch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class PullCordService extends Service {
    private static final String CHANNEL_ID = "pull_cord_torch";
    private static final int NOTIFICATION_ID = 41;
    private WindowManager windowManager;
    private PullCordView pullCordView;
    private WindowManager.LayoutParams windowParams;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean torchOn = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        findFlashCamera();
        showPullCord();
    }

    private void findFlashCamera() {
        try {
            for (String id : cameraManager.getCameraIdList()) {
                Boolean hasFlash = cameraManager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(hasFlash)) {
                    cameraId = id;
                    return;
                }
            }
        } catch (CameraAccessException ignored) { }
    }

    private void showPullCord() {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        pullCordView = new PullCordView();
        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        windowParams = new WindowManager.LayoutParams(
                dp(76), dp(220), overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        windowParams.gravity = Gravity.TOP | Gravity.END;
        windowParams.x = dp(5);
        windowParams.y = dp(72);
        windowManager.addView(pullCordView, windowParams);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toggleTorch() {
        if (cameraId == null) {
            Toast.makeText(this, "No flashlight found on this device.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            torchOn = !torchOn;
            cameraManager.setTorchMode(cameraId, torchOn);
            if (pullCordView != null) pullCordView.invalidate();
        } catch (CameraAccessException | SecurityException ex) {
            torchOn = false;
            Toast.makeText(this, "Unable to control flashlight. Close any app using the camera and try again.", Toast.LENGTH_LONG).show();
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setContentTitle("Pull Cord Torch")
                .setContentText("Floating pull-cord is active. Pull down to toggle the flashlight.")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Pull Cord Torch", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (torchOn && cameraManager != null && cameraId != null) {
            try { cameraManager.setTorchMode(cameraId, false); } catch (Exception ignored) { }
        }
        if (windowManager != null && pullCordView != null) {
            try { windowManager.removeView(pullCordView); } catch (Exception ignored) { }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private class PullCordView extends View {
        private final Paint rope = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint detail = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float downRawY;
        private int originalWindowY;
        private boolean triggered;

        PullCordView() {
            super(PullCordService.this);
            rope.setColor(0xFF8B6A45);
            rope.setStrokeWidth(dp(3));
            rope.setStyle(Paint.Style.STROKE);
            rope.setStrokeCap(Paint.Cap.ROUND);
            handle.setColor(0xFF8B6A45);
            handle.setStyle(Paint.Style.FILL);
            detail.setColor(0xFFD8C3A5);
            detail.setStyle(Paint.Style.STROKE);
            detail.setStrokeWidth(dp(2));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cordEnd = dp(148);
            canvas.drawLine(cx, 0, cx, cordEnd, rope);
            canvas.drawCircle(cx, cordEnd + dp(8), dp(13), handle);
            canvas.drawCircle(cx, cordEnd + dp(8), dp(8), detail);
            canvas.drawLine(cx - dp(4), cordEnd + dp(8), cx + dp(4), cordEnd + dp(8), detail);
            canvas.drawLine(cx, cordEnd + dp(4), cx, cordEnd + dp(12), detail);
            if (torchOn) {
                handle.setColor(0xFFDAA520);
                canvas.drawCircle(cx, cordEnd + dp(8), dp(13), handle);
                handle.setColor(0xFF8B6A45);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downRawY = event.getRawY();
                    originalWindowY = windowParams.y;
                    triggered = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float delta = event.getRawY() - downRawY;
                    if (!triggered && delta > dp(65)) {
                        triggered = true;
                        toggleTorch();
                        windowParams.y = originalWindowY;
                        windowManager.updateViewLayout(this, windowParams);
                    } else if (!triggered && delta > 0) {
                        windowParams.y = originalWindowY + (int) delta;
                        windowManager.updateViewLayout(this, windowParams);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    windowParams.y = originalWindowY;
                    windowManager.updateViewLayout(this, windowParams);
                    invalidate();
                    return true;
            }
            return true;
        }
    }
}
