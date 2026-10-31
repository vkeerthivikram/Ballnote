# Changelog

## Unreleased

### Added

- Independent +1/+2 wide and no-ball penalties, saved with each match.
- Over-completion messages and haptics, a large scoreboard, and automatic innings recaps.
- Bilateral and open-ended series, tri-series, leagues, knockouts, league playoffs, and group-stage tournaments.
- Fixture previews and shuffling, points tables and NRR, qualification, byes, organiser decisions, replay, withdrawals, and competition image sharing.
- Protection against corrections and deletions changing participants in later competition rounds.
- Room version 2 migration and backup version 2 with competition data; version 1 backups remain importable.

## [1.1.0] - 2026-10-31

### Added

- Runs and wicket totals for each over, including extras. Totals update after Undo and corrections.
- Delivery rows in match details. Tap a delivery to edit it.
- Different colours for fours and sixes, with red for wickets and labels for extras.
- Individual and selected-match deletion with Undo. Settings also has a separately confirmed option to delete all matches, including unfinished games.
- Status colours and labels on match cards.

### Changed

- Removed the Teams tab, saved-team suggestions and team records. Names now belong to individual matches, so changing sides does not affect other matches.
- Rematch copies the previous match's names and rules, which you can change before starting.
- Shared result images have separate innings panels, a winner highlight, and colours for completed, tied and no-result matches. Long names wrap in the result text.
- Updated the README and app screenshots.

### Compatibility

- Existing saved matches and version 1 JSON backups remain supported. The database format has not changed.

### Fixed

- Settings now shows the installed app's version instead of a fixed version label.

## [1.0.0] - 2026-10-31

- First signed APK release for Android 8.0 and later.
- Local scoring for both innings, adjustable overs and wicket limits, and three extras rules.
- Automatic saving, chase targets and results, delivery corrections, and match history.
- Saved teams and team records.
- Light, dark and system appearance, with optional wallpaper colours on Android 12 and later.
- Local result-image sharing and manual JSON backup and restore.

[1.1.0]: https://github.com/vkeerthivikram/Ballnote/releases/tag/v1.1.0
[1.0.0]: https://github.com/vkeerthivikram/Ballnote/releases/tag/v1.0.0
