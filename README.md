# Advance Launcher v2.0 — Daily Driver Edition (by SAROTECH)

**Developed by Prashant Latta • Brand: SAROTECH**

A clean-room, high-stability Android Home Launcher powered by a **Rust Native Core (`advance_core`)** and **Jetpack Compose**, built with **Glass & 3D Neomorphism** themes and full daily-driver features.

## Features
- **Rust Native Core (`rust-core/`)**: Crash-safe engine (`arm64-v8a`, `armeabi-v7a`) powering fuzzy + acronym app search, instant math calculator, smart app auto-categorizer, multi-cell widget/app grid collision solver, and JSON backup validator.
- **Glass & 3D Neomorphism Themes**: `GlassDark`, `GlassLight`, `NeomorphDark`, `NeomorphLight` with live frost opacity and 3D shadow depth controls.
- **Real Android Widgets (`AppWidgetHost`)**: Add any system or third-party app widget, resize columns/rows, and manage layout cleanly.
- **Notification Badges**: Live unread counts or minimal dots on app icons and folders via `NotificationListenerService`.
- **Direct Drag-and-Drop**: Press-and-drag icons anywhere on the home grid, drop onto another icon to auto-create a folder, or drop into an existing folder.
- **Play Store Icon Packs**: Full `appfilter.xml` icon pack loader supporting ADW, Nova, Apex, and Go icon packs.
- **Double-Tap Screen Lock**: Privacy-safe `AccessibilityService` (`canRetrieveWindowContent="false"`) for instant screen lock.
- **One-Handed A–Z Fast Scroller**: Quick thumb rail in App Drawer with live letter bubble.

## Build APK Automatically on GitHub
1. Push all files inside `AdvanceLauncher-SAROTECH` to your GitHub repo (`main` branch).
2. Open **Actions** → **Build Advance Launcher APK (Rust + Android)**.
3. Download **`Advance-Launcher-SAROTECH-APK`** from Artifacts and install on Android 8.0+ (API 26+).
