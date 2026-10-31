# Composition brief

## Identity and source assets

Ballnote, a local-only team cricket scorer for Android. Tagline: Tap. Score. Play.

Use these reviewed local sources:
- README.md: current features, installation, offline behaviour and distribution link.
- DESIGN.md: blue/ivory/native Material identity.
- app/src/main/java/com/ballnote/app/domain/Scoring.kt: runs, wickets, extras and deterministic over calculations.
- app/src/main/java/com/ballnote/app/ui/Ballnote.kt: scoring controls, saving and undo.
- app/src/main/AndroidManifest.xml: local-only app permission configuration.
- docs/screenshots/scoring-light.png and scoring-dark.png: actual native app visuals with fictional demo teams.
- app/src/main/res/drawable/ic_ballnote.xml: existing lowercase-b app mark, if needed; reproduce its exact vector paths rather than inventing a logo.

Never inspect or copy credentials, .signing/, environment files, private configuration, customer data or dependency trees into the video.

## Technical contract

- 1920 × 1080; 30 fps; root duration 20 seconds; 600 video frames.
- Use installed/pinned Hyperframes documentation for its HTML, clip timing and paused GSAP timeline contract. Scene hosts must be editable and all animation seekable.
- Local PNG assets, local fonts and locally generated audio only; no network calls in a rendered frame.
- Keep key text in the central 80% safe region and phone visuals within the central 90% action-safe region.
- Baseline text contrast at least 4.5:1, large text at least 3:1. Run the actual contrast checker and inspect coverage; do not disable checks.
- No misleading UI simulation: native stills can move, crop and receive emphasis overlays, but do not pretend an unrecorded state transition is captured footage.

## Timing and copy

0–3: “Less counting. More cricket.”
3–7: “Ballnote” and “Tap. Score. Play.” with the real light-mode screenshot.
7–12: “One tap per ball.” and “Runs. Wickets. Extras.” with the real controls enlarged.
12–16: “Your match. Your phone.” and “Offline. Auto-saved. Easy to undo.” with the real dark-mode screenshot.
16–20: “Ballnote”, “Download for Android”, “github.com/vkeerthivikram/Ballnote”.

## Visual tokens

Ivory #FAF8FF; navy #10244F; primary blue #4C609B; pale blue #DCE2FF; dark surface #11131B; dark primary #B4C5FF. Use reviewed local sans fonts. Keep the phone upright and the real screenshot proportions intact.

## Audio and rights

Audio was synthesized locally with composition/synthesize-audio.mjs and saved as composition/assets/original-score.wav: 20 seconds, 48 kHz stereo, 96 BPM. No sampled recordings, narration, external generation or paid services. The final AAC track has measured mean volume -25.6 dB and peak -7.2 dB, with no clipping. Rights/provenance are recorded in composition/ASSET-RIGHTS.md.

Final supporting copy was shortened for reading time: the reveal says “Made for turf cricket.”; the dark scene label says “LOCAL ONLY”; the ending says “Cricket scorer.” and “Download for Android”. The central headlines and destination remain as specified above.

## Required verification

Run Hyperframes checks with visible-text contrast coverage, inspect stills from all five scenes and transition boundaries, render locally, inspect decoded frames and audio, and verify with ffprobe. Extract a strong settled frame as brag.jpg and replace only frame zero in a new encode; preserve the first render and compare duration, frame count and audio timing before delivery.
