# Validation — 30 October 2026

Delivered APK: `outputs/Ballnote-debug.apk` (debug signed, Android API 26+).

SHA-256: `D05AB226840735AC82EF2836EE24302B6B2300BC70717325CE3233787A3CFF3E`

## Automated checks

- `testDebugUnitTest`: 14 tests, zero failures.
- `connectedDebugAndroidTest`: 3 tests, zero failures on an Android 11 / API 30 x86_64 Pixel 4 emulator.
- `assembleDebug`: successful.
- `lintDebug`: zero errors; 17 warnings (pinned dependency versions and a bitmap KTX style suggestion).
- APK permission inspection: no INTERNET permission. Only the AndroidX generated receiver permission is requested.

Device tests played a one-over match, recorded and undid a wide, started the chase, won with a six, corrected that six to one run, verified the reopened chase, and opened Rematch. They also closed/reopened Room, checked replacement backup restoration and rejection without data changes, and generated/read a 1080×1080 result image through FileProvider.

The emulator reported `Active default network: none` during the device checks.

## Native inspection

Inline design review; no native detector ran. Approved reference: the earlier Material You prototype. Source and native phone captures were inspected directly.

- Light and dark screenshots: `outputs/scoring-light.png`, `outputs/scoring-dark.png`.
- At standard font size, the score, run keys, extras, More, Undo and save status fit the portrait phone viewport.
- At font scale 1.3, the score switches to a stacked layout without clipped numbers; screenshot: `outputs/large-text.png`.
- Wide phone viewport checked: `outputs/landscape.png`. The vertically scrolling layout retains the scoreboard and controls; controls are below the initial fold in this short viewport.
- Force-stop/relaunch recovered the corrected chase: 1/0 after 0.1 overs, target 5, four needed from five balls.
- Emulator font scale and viewport size were restored after inspection.

Review disposition: ship for group testing. The blue scoreboard, light/dark colour roles and fixed scoring-key order match the approved direction. Moving More and Undo onto one row resolved the initial portrait fold issue. Design tokens are recorded in `DESIGN.md` and `.impeccable/design.json`.

## Remaining hands-on checks

Physical-device performance, outdoor readability, TalkBack operation, Android 12+ wallpaper colours, and choosing real file-picker/share destinations still need hands-on testing. The image generation, backup encoding/validation and Room restore paths are covered; external receiving apps and document providers are not automated here. Store publication is outside this delivery.

## Repeat

`build.ps1` uses the project-local toolchain when available. On another machine, install JDK 21 and SDK 36 and use `gradlew.bat testDebugUnitTest assembleDebug lintDebug`.

For device tests, use a clean test installation: `adb shell pm clear com.ballnote.app` before `gradlew.bat connectedDebugAndroidTest`. This deletes only that test device's Ballnote data; export a backup before doing it on a device with real matches. Tests retain the APK for screenshot inspection.
