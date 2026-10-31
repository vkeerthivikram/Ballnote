# Saved data and compatibility

Read this guide before changing persistence, serialized models, backup import/export, deletion or undo. The implementation lives in [Storage.kt](../../app/src/main/java/com/ballnote/app/data/Storage.kt); historical schemas are in [app/schemas](../../app/schemas/).

## Compatibility requirements

- Preserve existing matches and import compatibility with version 1 backups. Inspect the current Room and backup versions in `Storage.kt` before changing their formats.
- Provide explicit Room migrations and commit the generated schema when the database structure changes. Do not use destructive migration as an upgrade strategy or rewrite historical schema files.
- Give new serialized fields compatible defaults where appropriate. Serialized names, enum values and IDs are persisted contracts.
- Validate imported data and cross-references before replacing anything. Restore is a confirmed, atomic replacement of match/competition data; preferences are stored separately.
- Keep match/fixture creation and related writes transactional. Preserve deletion undo and retry behaviour.
- Android cloud backup/device transfer are disabled. Manual export is the recovery path; do not clear real user data to resolve a development problem.
