# Releasing Ballnote

## Publish an APK

1. Open [Actions → Release APK](https://github.com/vkeerthivikram/Ballnote/actions/workflows/release.yml) and choose **Run workflow** on `main`.
2. Enter a new version without the `v` prefix, such as `1.0.1`. Set the prerelease option if needed.
3. Leave **Publish a tag and GitHub Release** selected. Uncheck it for a build-only run with downloadable workflow artifacts.

The workflow runs unit tests and release lint, builds and verifies a signed APK, then publishes `Ballnote-v<VERSION>.apk` and `SHA256SUMS.txt`. Pushing a commit or tag does not run it.

A tag such as `v1.0.1` identifies the source commit; its GitHub Release holds the downloads. An existing tag cannot be moved by the workflow. Retrying a release is supported only from the same commit; changed source requires a new version.

Android's `versionCode` is `1000 + GITHUB_RUN_NUMBER`. Preserve increasing version codes if the workflow or repository changes so installed copies can receive updates.

## Signing configuration

Configure these repository Actions secrets before running the workflow:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The release JKS file, encoded as base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Signing key alias |
| `ANDROID_KEY_PASSWORD` | Signing key password |

Keep a private backup of the release key and credentials. The same key is required to sign compatible updates. Keystores, credentials and local signing files must not be committed or uploaded as release assets.

For a local signed build, the Gradle configuration reads `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` from the environment. `ANDROID_KEYSTORE_PATH` should be an absolute path to the keystore.

## Installing updates

Release APKs signed with the same key can update an installed release normally.

Debug builds use a different signing key. To switch from a debug build to a release, export a match backup, uninstall the debug app, install the release APK, then restore the backup. Uninstalling removes local app data.
