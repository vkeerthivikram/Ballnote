# Documentation and releases

Read this guide for user-visible changes, signing, packaging, installation or publishing.

- Update `README.md` and the Unreleased changelog for user-visible behaviour changes. Keep rules described in setup, sharing and documentation consistent with the engine.
- Keep `.tools/`, build output, `local.properties`, signing material and credentials untracked. Never print signing secrets or commit APKs/keystores.
- Follow [the release guide](../releasing.md) and [the release workflow](../../.github/workflows/release.yml) for signed releases, versioning and publishing. Preserve the signing key and increasing version codes for compatible updates.
- Debug and release signing keys differ. Preserve/export data before any install procedure that requires uninstalling an existing app.
- Distinguish APK creation from release publishing when reporting results. Building an APK alone does not publish a release.
