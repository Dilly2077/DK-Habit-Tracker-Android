# DK Habit Tracker — Android v0.1

A private, local-first Android habit tracker with the kinds of features commonly placed behind subscriptions in mainstream habit apps. No account, ads, telemetry, analytics SDK, cloud backend or network permission.

## Download the APK

After this project is pushed to GitHub, the **Publish Latest APK** workflow automatically creates/updates a GitHub Release named **Latest Android APK**. Download `DK-Habit-Tracker.apk` from the repository's **Releases** section.

The **Build Android APK** workflow also stores the same APK as a GitHub Actions artifact for 30 days.

> Android may ask you to allow installs from your browser/file manager because this APK is distributed outside Google Play.

## Included in v0.1

- Unlimited habits.
- Daily, weekdays, or flexible `N times/week` schedules.
- Optional local reminder time per habit.
- One-tap completion from the Today dashboard.
- Current streak and best streak tracking.
- 30-day completion rate.
- 7-day progress chart.
- 10-week consistency heatmap.
- Categories and emoji habit icons.
- Archive/restore and permanent delete.
- JSON backup and restore via Android's document picker.
- System, Light, Dark and true OLED themes.
- Local-only storage using Android SharedPreferences + JSON.
- No INTERNET permission in `AndroidManifest.xml`.

## Privacy

Habit names, schedules and completion history are stored only in the app's private local storage unless the user explicitly exports a backup file. The app contains no advertising or analytics SDK and requests no network permission.

## Build locally

Requirements:

- Android Studio with Android SDK 35, or Gradle 8.9 + JDK 17 + Android SDK 35.

From the repository root:

```bash
gradle :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The debug build is Android-signed automatically and can be installed directly on a device.

## GitHub Actions

Two workflows are included:

- `.github/workflows/build-apk.yml` — validates every push/PR and uploads the APK artifact.
- `.github/workflows/publish-latest-apk.yml` — on every push to `main`/`master`, moves the `latest` tag and updates a persistent **Latest Android APK** release.

The release workflow requires GitHub Actions to have `contents: write`, which is declared in the workflow. If the repository has organization-level restrictions that override this, enable **Settings → Actions → General → Workflow permissions → Read and write permissions**.

## Project identity

- App name: **DK Habit Tracker**
- Package: `com.dk.habittracker`
- Min Android: API 26 / Android 8.0
- Target Android: API 35
- Version: `0.1.0`
