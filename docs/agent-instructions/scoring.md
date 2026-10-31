# Scoring and competitions

Read this guide when changing delivery handling, match outcomes, recaps, standings, fixtures or progression. The engines live in [Scoring.kt](../../app/src/main/java/com/ballnote/app/domain/Scoring.kt) and [Competition.kt](../../app/src/main/java/com/ballnote/app/domain/Competition.kt).

## Rules to preserve

- Recalculate totals, recaps and results from delivery history. Avoid a separate mutable score that can drift after undo or correction.
- Use integer counted balls for arithmetic. Displayed overs such as `4.2` mean four overs and two balls, not a decimal number of overs.
- Wide/no-ball penalties are independently configurable as +1 or +2. Penalty runs and whether a delivery counts as a ball are separate rules. Apply the match's rules consistently to scoring, previews, chips, recaps and sharing.
- Preserve all three extras modes, including the final-over exception. End innings at their ball/wicket limit or a successful chase; later deliveries require the existing correction-preview handling.
- Corrections may change targets, truncate excluded deliveries or reopen a completed match. Keep these effects visible before applying them.
- Recap dots are scoreless non-wicket deliveries. Fours/sixes describe recorded run outcomes, including no-ball batting runs, rather than independently verified boundaries.
- Current league points are win 2, tie/no result 1, loss 0. Rank by points, wins, NRR, then entry order. NRR uses counted balls, full allocation for all-out innings, and excludes early-closed innings and organiser-awarded results. These are the app's documented turf rules; changes need matching domain tests and documentation.
- Preserve fixture IDs and dependency order. Handle byes without inventing played matches. Tied knockouts use replay or an organiser decision with a reason.
- Corrections, deletion, replay and decisions must respect qualification locks and participants in already scored or decided later rounds. Keep progression guards in domain/repository logic, not only in UI buttons.
- Replays, knockout resets and competition deletion retain recorded match history. Preserve that distinction when changing deletion behaviour and undo.
