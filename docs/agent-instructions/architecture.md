# Product and architecture

Read this guide when changing product behaviour, app structure or UI state management. Paths are relative to the repository root unless noted.

## Product intent

Ballnote is an offline Android scorer for casual turf cricket. Its promise is **Tap. Score. Play.**

- Keep the common delivery outcome one tap, with immediate feedback, automatic saving and accessible undo.
- Track team totals. Player profiles, individual batting/bowling statistics, voice and camera scoring require a user request.
- Keep quick matches easy to start. Competition setup belongs outside the delivery-scoring flow.
- Preserve offline first launch and operation. The app currently has no internet permission, accounts, advertising or analytics; changes to that model must be part of the requested scope.
- Session timers are opt-in and independent. Support match-only, tournament-only, both and neither. Creating a match/competition must not start a timer automatically; a tournament timer must not become a fixture's own timer.
- Team names belong to individual matches or competition entrants. Preserve historical names and stable IDs; matching display names do not establish shared identity.

## Code map

All application Kotlin sources are under `app/src/main/java/com/ballnote/app/`.

| Location | Responsibility |
| --- | --- |
| `domain/Scoring.kt` | Serializable match models, delivery replay, innings, targets, results and correction previews |
| `domain/Competition.kt` | Series/tournament models, fixtures, standings, NRR, qualification, byes and progression guards |
| `domain/SessionTimer.kt` | Saved countdown state, timer arithmetic, and immutable live snapshot text |
| `timers/TimerAlerts.kt` | Background reminder scheduling, notification delivery and reboot handling |
| `ui/SessionTimer.kt` | Match/tournament timer controls and countdown display |
| `data/Storage.kt` | Room entities/DAO, repository transactions, backup validation/restore and DataStore preferences |
| `AppModel.kt` | StateFlow state, serialized mutation commands, retries and UI events |
| `ui/Ballnote.kt` | App navigation, match setup, live scoring, details, settings and delivery editing |
| `ui/Competitions.kt` | Competition setup, previews, fixtures, tables and organiser actions |
| `ui/MatchComponents.kt` | Match cards, status styling and delivery/over components |
| `ui/ResultImage.kt`, `ui/Scoreboard.kt` | Shared images and the large scoreboard |
| `app/schemas/` | Versioned Room schemas |
| `app/src/test/` | JVM scoring, competition and backup tests |
| `app/src/androidTest/` | Device UI, repository, migration and image-generation tests |

Keep scoring and competition calculations in plain Kotlin domain code. Compose renders derived state; the repository owns persistence. Route mutations through the existing command/transaction mechanisms so retries and concurrent taps remain safe. Observe StateFlow in composition through Compose state collection.
