# Ballnote

Ballnote is an offline Android app for team-only turf cricket scoring, series and tournaments. Its promise is **Tap. Score. Play.**

- Preserve one-tap scoring, automatic saving and offline operation.
- Preserve existing match data, historical identities and backup compatibility.
- Preserve existing uncommitted work when making changes.

## Quick commands

Run from the repository root with JDK 21, Android SDK 36 and Build-Tools 36.0.0:

- Android build/checks: `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug`
- Device tests: `.\gradlew.bat connectedDebugAndroidTest`
- On other platforms use `./gradlew`. See the testing guide for local tooling and test prerequisites.

## Task-specific instructions

Read the relevant guides before changing their areas:

- [Product and architecture](docs/agent-instructions/architecture.md): product scope, code map and state management.
- [Scoring and competitions](docs/agent-instructions/scoring.md): delivery rules, results, standings and progression safeguards.
- [Saved data and compatibility](docs/agent-instructions/data.md): migrations, serialization, backups and deletion.
- [Build and verification](docs/agent-instructions/testing.md): checks by change type, emulator setup and troubleshooting.
- [Documentation and releases](docs/agent-instructions/releases.md): user-facing docs, signing and publishing.
