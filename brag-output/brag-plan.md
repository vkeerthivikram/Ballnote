# Ballnote launch film

## Direction

- Product: **Ballnote — Cricket Scorer**. Existing tagline: **Tap. Score. Play.**
- Audience: people playing casual turf cricket who want quick team-only scoring.
- Angle: less counting, more cricket. Show the actual app before explaining features.
- Format: landscape, 1920 × 1080, 30 fps, exactly 20 seconds / 600 frames.
- Look: Ballnote's ivory, navy and tonal blue; large clean type; deliberate cuts and restrained movement. No invented trophies, endorsements, user counts or Play Store listing.
- Evidence: README.md, DESIGN.md, domain/Scoring.kt, ui/Ballnote.kt, AndroidManifest.xml, and committed screenshots under docs/screenshots/.
- Demo data: the existing screenshots contain fictional Weekend XI match data, 4/0 at 0.1 overs. They are genuine native emulator captures, not newly invented app layouts.

## Supported claims

One-tap runs/wickets/extras; automatic score and over calculations; automatic saving; undo; local-only scoring; Android APK distribution on GitHub. No automatic camera/voice scoring, player statistics, or Play Store availability claims.

## Timed storyboard

| Time | Picture | Exact headline / supporting copy |
| --- | --- | --- |
| 0.0–3.0 | Ivory opening, bold navy words; delivery-dot rhythm introduces the real screenshot | Less counting. / More cricket. |
| 3.0–7.0 | Reveal the actual light-mode scoring screen in a clean phone frame | Ballnote / Tap. Score. Play. |
| 7.0–12.0 | Move closer to the genuine scoring controls; subtle highlight of 4, Wicket and Wide | One tap per ball. / Runs. Wickets. Extras. |
| 12.0–16.0 | Dark-mode screen; focus on the score and Undo | Your match. Your phone. / Offline. Auto-saved. Easy to undo. |
| 16.0–20.0 | Settled brand ending with the real product and download destination | Ballnote / Download for Android / github.com/vkeerthivikram/Ballnote |

Scene durations total 20 seconds. Hold settled copy for at least 0.3 seconds per word; transitions must not consume the reading interval. No narration requested.

## Audio

Plan an original, locally synthesized 96 BPM instrumental pulse plus restrained tap accents; no samples, external recordings or paid generation. Save its synthesis source and document that it was created for this film. If it cannot be produced reliably, explicitly report silent output instead of implying audio exists.

## Rendering prerequisites

Installed with Vicky's explicit authorization inside composition/: hyperframes 0.8.223, ffmpeg-static 5.3.0 (FFmpeg 6.1.1), ffprobe-static 3.1.0 (ffprobe 4.0.2), and GSAP 3.13.0. Node 22.22.2 and the installed local Chrome were used. No global installs, hosted rendering, project uploads or paid services.

## Completed validation

All required deliverables were produced locally. The full Hyperframes check passed with 21/21 contrast checks and no lint, runtime, layout or audio errors. Scene and transition stills were inspected before rendering; decoded video frames and frame zero were inspected afterward. ffprobe verified exactly 20 seconds, 1920×1080, 30 fps and 600 frames. The original 20-second stereo AAC audio was preserved byte-for-byte through the poster replacement. See REVIEW.md and validation.json.

## Delivery

brag.mp4, brag.jpg, this plan, composition-brief.md, share-copy.txt and reusable composition/. Check contrast coverage and each scene/transition before rendering. Validate final duration, dimensions, frame count and audio, then replace frame zero with a settled poster while preserving the original render for comparison.
