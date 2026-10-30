# Publishing an APK

Open **Actions → Release APK → Run workflow** on GitHub. Choose `main`, enter a version without the `v` prefix (for example `1.0.0`), and run it. Leave **Publish a tag and GitHub Release** checked to publish. Uncheck it for a build-only run that saves a downloadable workflow artifact.

The workflow runs unit tests and release lint, builds a signed APK, verifies its signature, and attaches `Ballnote-v<VERSION>.apk` and `SHA256SUMS.txt` to the matching GitHub Release. It only runs manually; pushes and tags do not trigger publication.

A tag such as `v1.0.0` identifies the exact commit. The Release is its download page. Use a new version for changed code. A retry can upload assets for the same commit, but the workflow will never move an existing tag to another commit.

Android versionCode increases with the workflow run number (1000 + run number). Future workflows must preserve this monotonic sequence if the workflow or repository is replaced.

## Signing

The repository needs these Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: base64 of the release JKS file.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: key alias.
- `ANDROID_KEY_PASSWORD`: key password.

This workspace keeps the original key and credentials under `.signing/`, which is ignored by Git. Keep a private backup of that folder: losing the key prevents signing compatible updates. Never commit it or include it in a release.

The release key differs from the earlier local debug APK's key. To switch from that debug build, export your match backup, uninstall it, install the release APK, and restore the backup. Subsequent release APKs use the same release key and can update normally.

No Play Store or server configuration is required. Installing the APK is the only setup users need; scoring remains local.
