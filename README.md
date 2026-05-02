SmartGallery — Local Jetpack Compose Gallery

Quick start

1. Prerequisites
- Android SDK installed (e.g. C:\Users\profe\AppData\Local\Android\Sdk)
- `platform-tools` and `emulator` available (ensure SDK's `platform-tools` and `emulator` folders are in PATH or use full paths)
- JDK 17 (the assistant installed Eclipse Temurin 17 via `winget` if needed)

2. Build the debug APK

Open PowerShell in the project root and run:

```powershell
cd "C:\Users\profe\AndroidStudioProjects\SmartGallery"
./gradlew.bat assembleDebug
```

3. Start an emulator (or connect a device)
- Start an existing AVD via Android Studio Device Manager, or:

```powershell
& "C:\Users\profe\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd <AVD_NAME>
```

4. Install the APK and run

```powershell
$env:PATH += ";C:\Users\profe\AppData\Local\Android\Sdk\platform-tools"
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.smartgallery/.MainActivity
```

5. Add test images (optional)
Copy local images into the emulator so the gallery shows photos:

```powershell
$env:PATH += ";C:\Users\profe\AppData\Local\Android\Sdk\platform-tools"
$src = "C:\Users\profe\Pictures\TestImages"
adb shell "rm -f /sdcard/Pictures/* /sdcard/DCIM/Camera/* /sdcard/Download/*"
Get-ChildItem $src -Include *.jpg,*.jpeg,*.png,*.webp -File -Recurse | ForEach-Object { adb push $_.FullName "/sdcard/Pictures/" }
adb shell "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/"
```

6. Notes
- If you see a runtime permission prompt, grant `READ_MEDIA_IMAGES` (Android 13+) or `READ_EXTERNAL_STORAGE` for older devices.
- If the emulator can't start due to disk or memory, create a smaller AVD or free host disk space.

If you want, I can now: start a fresh AVD, finish a quick visual polish for the album strip, or open a PR with changes. Which should I do next?