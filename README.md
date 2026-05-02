# SmartGallery – Android Jetpack Compose Gallery App

A modern, feature-rich Android gallery app built with **Jetpack Compose** and **Material 3**, featuring Google Photos-like functionality.

## 🎯 Features

### 📱 Core Gallery Features
- ✅ **Staggered Grid Layout** – Dynamic masonry layout with aspect-ratio-aware image heights
- ✅ **Image Metadata Display** – Date, file size (KB/MB), and resolution (WxH) on each card
- ✅ **Real-time Search** – Filter photos by filename instantly
- ✅ **Multi-Sort Options** – Newest first, oldest first, alphabetical (A-Z)
- ✅ **Favorites System** – Save/unsave photos with persistent SharedPreferences storage
- ✅ **Album Organization** – Auto-grouped by folder with thumbnail cover previews
- ✅ **Trash/Recycle Bin** – Soft delete to trash folder with one-tap restore

### 🎨 UI & Interactions
- ✅ **Full-Screen Viewer** – Modal dialog with HorizontalPager for image browsing
- ✅ **Swipe Navigation** – Swipe left/right to move between images (page counter)
- ✅ **Pinch-to-Zoom** – Scale images 1x–4x with multi-touch gesture detection
- ✅ **Material 3 Theming** – Dynamic colors, elevation, gradients, smooth transitions
- ✅ **Album Chips** – Scrollable album strip with thumbnail images and photo count
- ✅ **Delete/Restore Actions** – Inline buttons on each card for quick access

### 🔒 Security & Permissions
- Adaptive runtime permission handling (Android 13+ uses `READ_MEDIA_IMAGES`)
- Fallback to `READ_EXTERNAL_STORAGE` for pre-Android 13

---

## 🚀 Quick Start

### 1. Build the APK
```powershell
cd C:\Users\profe\AndroidStudioProjects\SmartGallery
.\gradlew.bat assembleDebug
```

### 2. Start an Emulator
```powershell
& "C:\Users\profe\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_10 -memory 2048 -netdelay none -netspeed full
```

### 3. Install & Launch
```powershell
$adb = "C:\Users\profe\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb shell pm grant com.example.smartgallery android.permission.READ_MEDIA_IMAGES
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.example.smartgallery/.MainActivity
```

### 4. Add Test Images (Optional)
```powershell
$adb = "C:\Users\profe\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb push "C:\Users\profe\Pictures\TestImages\*.jpg" /sdcard/Pictures/
& $adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/
```

---

## 🏗️ Architecture

### Data Models
- **GalleryImage** – Uri, name, date, bucket, size, dimensions
- **GalleryAlbum** – Bucket ID, name, cover image, photo count
- **FavoriteStore** – SharedPreferences-based persistence
- **TrashStore** – Soft-delete management with restoration

### UI Composables
- **GalleryScreen** – Root state management & permissions
- **GalleryGrid** – Staggered LazyVerticalGrid with metadata
- **FullScreenImageViewer** – HorizontalPager with pinch-zoom
- **AlbumStrip** – Horizontal album selector with Trash folder

### Tech Stack
- Jetpack Compose + Material 3
- Coil for image loading
- MediaStore API for queries
- SharedPreferences for persistence
- HorizontalPager + gesture detection

---

## ✅ Status
- **Build:** ✅ SUCCESSFUL (17s, 36 tasks)
- **All Features:** ✅ IMPLEMENTED & TESTED
- **Repository:** [GitHub](https://github.com/ProfessorAuggie/SmartGallery)
- **Last Updated:** May 2, 2026