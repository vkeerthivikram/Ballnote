# Ballnote

Local-only Android cricket scorer, built with Kotlin, Compose Material 3, Room and DataStore. Android 8.0+ (API 26); compile/target API 36.

## GitHub releases

Use **Actions → Release APK → Run workflow** to build and publish a signed APK with a version tag. See [RELEASES.md](RELEASES.md) for instructions and signing details. The workflow can also run in build-only mode without publishing a release.

## Build

Open this folder in Android Studio with JDK 21 and Android SDK 36 installed. Allow Gradle to download build dependencies, then run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Install with `adb install -r <apk-path>`. Build tools require internet during initial setup; the installed app does not.

On this workspace, `./build.ps1` selects the downloaded project-local JDK and SDK automatically. The tested APK is also copied to `outputs/Ballnote-debug.apk`.

## Use

Create a match with two team names (or choose saved teams), batting order, overs, wicket limit and extras mode. Ordinary scoring is one tap. More supports unusual runs and wickets on extras. Undo reverses the latest delivery; Details lets you edit or delete any delivery, preview the effect and confirm removals beyond a newly reached innings endpoint.

Matches retain historical team names. Team records follow stable team IDs and update after corrections. Archiving a team retains its history. Rematch prefills the prior setup.

Settings provides manual JSON backup and replacement restore. Backups contain teams, matches, rules and deliveries, not appearance preferences. Restore supports format version 1, validates before replacement, and limits input to 25 MB. Keep a backup before uninstalling or changing phones. Share generates a local PNG and opens the Android Sharesheet.

## Data and privacy

No internet permission, servers, analytics, advertisements or remote assets. Android cloud backup and device transfer are excluded. Files leave the app only through user-triggered export/share. A system file picker may offer cloud storage destinations; the user controls the destination.

Database schema version 1 is exported under `app/schemas`. Future schema changes must supply a Room migration; destructive fallback is deliberately absent.

## Verification

Unit tests cover extras, final-over transitions, results, combined outcomes, correction previews/truncation/reopening, records and backup validation. Device tests cover Room persistence after reopening and replacement restore validation. Run device tests with `gradlew connectedDebugAndroidTest` when an emulator or phone is available. See `VALIDATION.md` for actual results and remaining checks.
