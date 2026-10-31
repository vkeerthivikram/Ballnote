# Build and verification

Read this guide when building the app, changing Kotlin code, testing UI/storage changes or diagnosing local tool failures. Commands run from the repository root.

## Toolchain and commands

Use JDK 21, Android SDK 36 and Build-Tools 36.0.0. The app supports Android 8.0/API 26 onward; JVM bytecode targets are configured separately in Gradle. Read the Gradle files for dependency versions rather than upgrading them incidentally.

From the repository root on Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

On other platforms use `./gradlew`. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

This workspace may have ignored local tools. When present, verify their paths before using them:

```powershell
$env:JAVA_HOME = "$PWD\.tools\jdk\jdk-21.0.13+9"
$env:ANDROID_HOME = "$PWD\.tools\sdk"
$env:GRADLE_USER_HOME = "$PWD\.tools\gradle-home"
.\.tools\gradle-8.13\bin\gradle.bat testDebugUnitTest lintDebug assembleDebug --offline
```

The direct Gradle installation is a fallback when the wrapper distribution is unavailable. Match it to `gradle/wrapper/gradle-wrapper.properties`; offline mode also requires cached dependencies. Gradle and ADB need working localhost connections. Diagnose sandbox/transport failures separately from application failures and use the environment's supported escalation mechanism when necessary.

- For Kotlin changes, run unit tests, lint and the APK build. Add meaningful regression cases for changed scoring, standings, progression, serialization or storage behaviour.
- For UI, persistence or migration changes, also run the relevant device tests on a dedicated emulator with a clean test installation. Tests create and delete sample matches. Verify the emulator serial before any data reset; do not reset a personal device.
- Exercise scoring, extras, undo, corrections, chase completion, restart persistence and affected competition flows. Check small screens, large text, themes, landscape and keyboard-open forms when relevant.
- In Compose tests, wait for asynchronous save/snackbar state. Scroll the containing vertical page to bring a target fully into view; horizontally scrollable delivery rows can otherwise leave taps at the screen edge.
- Start background emulators/helpers hidden and close helpers you started when finished. Record blockers and checks actually completed; do not claim a passing device run from a successful compilation.
- For documentation-only changes, verify references and `git diff --check`; Android builds are unnecessary.
