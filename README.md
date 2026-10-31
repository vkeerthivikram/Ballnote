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

## Competitions and series

Open **Competitions** to create a bilateral series, an open-ended session, a tri-series, a league, a knockout, league playoffs, or groups followed by knockouts. Enter 2–16 team names, choose common match rules, and preview the fixtures. Shuffle the teams in the preview to change pairings or group allocation before saving.

Bilateral series support a fixed match count or best-of format. Sessions have a Next match action and can be ended explicitly. Tri-series support single or double round-robin play with an optional top-two final. League playoffs support a top-two final, top-four semifinals, or qualifier/eliminator playoffs. Groups support 2–4 groups with two or four qualifiers per group.

Tap a fixture and choose who bats first. Scores save automatically and update the competition overview. League points are win 2, tie/no result 1, loss 0. Ranking uses points, wins, net run rate, then the original team entry order. NRR uses counted balls, treats all-out innings as their full allocation, and excludes early-closed innings and organiser-awarded results. Preview these rules before starting; this is a local turf competition format.

Use **Generate finals** after completing league fixtures. Tied knockouts can be replayed or resolved with a recorded organiser decision and reason. Withdrawn teams cannot start new fixtures; resolve those fixtures with decisions or reinstate the team. Reset knockout rounds before making a correction that changes qualified teams or participants in an already scored or decided later fixture. Resets and replays retain earlier match records in match history. Deleting a competition also retains its recorded matches.

Share the table, fixtures and results as one image. Competition data is included in exported backups. Version 1 backups can still be imported; newer exports require this updated app.

## Scoring additions

Choose **+1 or +2** penalties independently for wides and no-balls. These are separate from the rule deciding whether extras count as balls. Existing matches keep +1 rules.

A completed over produces a short message and a distinct vibration when haptics are enabled. Tap the score panel to open the large scoreboard; rotate the phone for landscape and use Back to scoring to return. The large scoreboard keeps its screen awake.

Match details includes an automatic recap: scoreless non-wicket deliveries, extras, recorded fours and sixes, and the highest-scoring over. Fours and sixes mean recorded run outcomes, including no-ball batting runs; the app does not distinguish a boundary from four or six runs completed by running. Recaps update after corrections and undo.

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
