# DK Habit Tracker v0.1 — checkpoint

## Completed

- Native Android Kotlin + Jetpack Compose project.
- Local-only habit persistence.
- Daily / weekdays / N-times-per-week schedules.
- Local notification reminders and boot rescheduling.
- Today dashboard, streaks, archive/delete.
- 30-day stats, 7-day chart, 10-week heatmap.
- JSON backup/restore.
- System/Light/Dark/OLED appearance modes.
- No INTERNET permission, telemetry, ads or account dependency.
- GitHub Actions build workflow.
- GitHub Actions persistent `latest` APK release workflow.
- Project pushed to `Dilly2077/Habit-Tracker-Android`.

## Build target

- Package: `com.dk.habittracker`
- Version: `0.1.0`
- minSdk: 26
- targetSdk / compileSdk: 35
- JDK: 17
- Gradle: 8.9
- AGP: 8.7.3
- Kotlin: 2.0.21

## Distribution

Every push to `main` triggers GitHub Actions. The build workflow uploads `DK-Habit-Tracker.apk` as an Actions artifact, and the release workflow updates the persistent `latest` release under **Releases → Latest Android APK**.
