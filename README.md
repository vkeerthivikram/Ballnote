# Ballnote

An Android cricket scorer for casual turf matches. Record runs, wickets and extras, follow the chase, and pick up where you left off. All match data is stored on your phone.

[Download APK](https://github.com/vkeerthivikram/Ballnote/releases/latest) · [Watch demo](https://github.com/user-attachments/assets/a3130143-ef43-4bdd-a125-e766d957c668) · [Report an issue](https://github.com/vkeerthivikram/Ballnote/issues)

<p>
  <img src="docs/screenshots/scoring-light.png" alt="Ballnote scoring screen in light mode" width="240">
  <img src="docs/screenshots/scoring-dark.png" alt="Ballnote scoring screen in dark mode" width="240">
</p>

## Install

Requires **Android 8.0 or later**. Download the APK from [Releases](https://github.com/vkeerthivikram/Ballnote/releases/latest), open it on your phone, and allow installation from that source if Android prompts you.

## Scoring

- Runs, wickets, wides and no-balls take one tap. Use **More** for additional runs or runs combined with a wicket.
- Set the overs, wicket limit and extras rule before starting. Defaults are six overs, five wickets, and extras bowled again.
- Wides and no-balls add one run. Choose whether they count as a ball, are bowled again, or are bowled again only in the final scheduled over.
- Chase targets, remaining balls and winning margins update automatically.
- Undo the last delivery, or edit any delivery in either innings. Corrections show a preview before applying changes.
- Resume saved matches, reuse teams, view team records, and share a result image.
- Use light, dark or system appearance, with optional wallpaper colours on Android 12+.

Scoring tracks team totals; individual batting and bowling statistics are outside the app's scope.

## Data and privacy

Ballnote works offline from first launch. It has no internet permission, accounts, advertising or analytics.

Use **Settings → Export backup** to save teams and matches as JSON. Restore validates the file and asks for confirmation before replacing the current data. It does not merge backups or restore appearance preferences.

Automatic Android cloud backup and device transfer are disabled. Export a backup before uninstalling or changing phones. Sharing and exporting use Android's system picker, so you choose the destination.

## Development

Built with Kotlin, Jetpack Compose, Material 3, Room and DataStore. The scoring engine is plain Kotlin; scores and team records are calculated from delivery history.

Open the project in Android Studio, or configure **JDK 21**, **Android SDK 36** and **Build-Tools 36.0.0** for command-line builds. Set `JAVA_HOME` and provide the SDK location through `ANDROID_HOME` or `local.properties`.

```sh
./gradlew assembleDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Tests

```sh
./gradlew testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest
```

The second command needs a connected emulator or test device with a clean Ballnote installation. Device tests create sample matches. Unit tests cover scoring rules, corrections, results and backup validation; device tests cover scoring, storage and result-image generation.

### Source layout

- `app/src/main/java/com/ballnote/app/domain/` — scoring rules and match calculations.
- `app/src/main/java/com/ballnote/app/data/` — Room storage, preferences and backups.
- `app/src/main/java/com/ballnote/app/ui/` — Compose screens.
- `app/schemas/` — versioned Room schemas for database migrations.

## Releases

The [Release APK workflow](https://github.com/vkeerthivikram/Ballnote/actions/workflows/release.yml) builds and publishes signed APKs when run manually. See [docs/releasing.md](docs/releasing.md) for signing, versioning and upgrade instructions.
