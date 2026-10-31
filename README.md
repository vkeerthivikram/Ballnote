# Ballnote

Ballnote keeps score for turf cricket matches on Android. Tap the outcome after each delivery, and the app updates the score and saves the match on your phone. It tracks team totals, not individual batting or bowling statistics.

[Download APK](https://github.com/vkeerthivikram/Ballnote/releases/latest) · [Changelog](CHANGELOG.md) · [Watch demo](https://github.com/user-attachments/assets/a3130143-ef43-4bdd-a125-e766d957c668) · [Report an issue](https://github.com/vkeerthivikram/Ballnote/issues)

<p>
  <img src="docs/screenshots/scoring-light.png" alt="Ballnote scoring screen in light mode" width="240">
  <img src="docs/screenshots/scoring-dark.png" alt="Ballnote scoring screen in dark mode" width="240">
</p>

## Install

Requires Android 8.0 or later. Open the downloaded APK on your phone. If Android prompts you, allow installation from the app you used to open it.

## Scoring a match

Choose the overs, wicket limit and extras rule before starting. The defaults are six overs, five wickets, and wides and no-balls bowled again for one extra run. You can instead count extras as balls, or count them until the final scheduled over and bowl them again in that over.

Tap a run total, wicket, wide or no-ball to record a delivery. Use **More** for additional runs or runs combined with a wicket. Ballnote calculates the chase target, remaining balls and winning margin.

The delivery row shows the current over's runs, including extras. Fours and sixes have different colours, and wickets are red. Match details groups deliveries by over with a total for each. Tap a delivery chip to edit it.

Use **Undo last ball** to remove the latest delivery. You can also edit a delivery in either innings. The app previews corrections before you apply them.

Matches save as you score, so you can close the app and resume later. Team names belong to each match. Use Rematch to copy the names and rules, then change them for the next game. You can share a match result as an image.

Match cards show their status in text and colour. Shared results have separate innings panels and highlight the winner.

To delete a match, open its card's menu or use **Delete match** in match details. Use **Select** on the Matches screen to delete several at once. The deletion message has an **Undo** action. In Settings, **Delete all matches** asks for confirmation before clearing the history, including unfinished matches.

The app supports light, dark and system appearance. On Android 12 or later, it can use colours from your wallpaper.

## Backups and privacy

Ballnote works offline, including on first launch. It has no internet permission, accounts, advertising or analytics.

In Settings, choose **Export backup** to save your matches as JSON. Restore checks the file and asks for confirmation before replacing your current matches. It does not merge data or restore appearance preferences.

Android cloud backup and device transfer are disabled, so export a backup before uninstalling or changing phones. Android's file picker and share sheet let you choose where to save or send exported data.

## Build and test

Open the project in Android Studio, or build from the command line with JDK 21, Android SDK 36 and Build-Tools 36.0.0. Set `JAVA_HOME` and provide the SDK path through `ANDROID_HOME` or `local.properties`.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
```

On Windows, use `.\gradlew.bat` instead of `./gradlew`. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

To run device tests, connect an emulator or test device with a clean Ballnote installation. The tests create sample matches.

```sh
./gradlew connectedDebugAndroidTest
```

Unit tests cover scoring rules, corrections, results and backup validation. Device tests cover scoring, storage and result-image generation.

## Code

The app uses Kotlin, Jetpack Compose and Material 3. Room stores match data, and DataStore stores preferences. The scoring engine calculates scores and match results from delivery history.

- [Scoring.kt](app/src/main/java/com/ballnote/app/domain/Scoring.kt) contains the scoring engine, which is plain Kotlin.
- [Storage.kt](app/src/main/java/com/ballnote/app/data/Storage.kt) handles the database, preferences and backups.
- [Ballnote.kt](app/src/main/java/com/ballnote/app/ui/Ballnote.kt) defines the Compose screens.
- [app/schemas](app/schemas/) contains versioned Room schemas for database migrations.

## Releases

Run the [Release APK workflow](https://github.com/vkeerthivikram/Ballnote/actions/workflows/release.yml) to build and publish a signed APK. It only runs manually. See [docs/releasing.md](docs/releasing.md) for signing, versioning and upgrade instructions.
