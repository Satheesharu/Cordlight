# Pull Cord Torch

A small Java Android Studio starter project for a traditional pull-cord flashlight overlay.

## Open and build
1. Open this folder in Android Studio.
2. Allow Gradle sync to finish (Android Gradle Plugin 8.7.3, JDK 17).
3. Connect your OPPO phone with USB debugging enabled.
4. Run the `app` configuration.

## First launch
1. Tap **Enable pull-cord**.
2. Grant Camera permission.
3. Allow **Display over other apps** in Android settings.
4. Return to the app and tap **Enable pull-cord** again.
5. A traditional cord appears near the top-right. Drag the handle down by about 65 dp to toggle the torch.
6. Open the app and tap **Hide pull-cord** to remove it.

The ongoing notification indicates that the overlay service is active. The service turns the torch off when it is stopped.

## Notes
- This is Android source code, not a prebuilt APK.
- Android/OPPO background and overlay restrictions may vary by OS version.
- The app needs Camera permission because Android exposes the flashlight through CameraManager.
- If another app is actively using the camera, Android may reject torch control.
