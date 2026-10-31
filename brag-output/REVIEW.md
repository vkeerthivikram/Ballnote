# Ballnote launch video — completed review

## Result

brag.mp4 is a locally rendered landscape H.264/AAC MP4. Exactly 1920×1080, 30 fps, 600 frames and 20 seconds. Size: 1,590,326 bytes. No narration was requested or added.

## Product truth and visuals

- Real native Ballnote screenshots from docs/screenshots are used in the reveal, keypad close-up, dark scene and ending.
- The screenshot state is fictional test data: Weekend XI at 4/0 after 0.1 overs. This is an actual captured screen state, not a fabricated interface.
- Tap rings are editorial highlights over still screenshots. The film does not claim to be a live screen recording or show invented score transitions.
- Claims about one-tap scoring, runs/wickets/extras, local saving and undo were checked against the app source and README.
- The film uses the existing Ballnote identity, vector app mark, blue/ivory palette and Roboto font.
- No invented user counts, endorsements, testimonials or Play Store availability.

## Pre-render checks

Hyperframes 0.8.223 check-final.json:
- Lint: zero errors, zero warnings.
- Runtime: zero errors, zero warnings.
- Layout: zero errors across 16 explicit sample times, including both sides of each scene cut and all three tap highlights.
- Contrast: enabled, 21 elements checked, 21 passed across all five scenes. Native screenshot text was also visually inspected; raster text is not included in DOM contrast coverage.
- Audio: zero errors, zero warnings.
- Reviewed all 16 captured frames and both contact sheets under review/frames/.
- Local editable preview returned HTTP 200 before final rendering.

## Final media checks

- Native render completed in 46.7 seconds. Original preserved as brag-original.mp4.
- Reviewed decoded-contact.jpg from the finished MP4, covering frame zero and every scene.
- Poster selected from the settled hero at 4.8 seconds and saved as brag.jpg.
- Replaced only frame zero; retained exactly 600 frames and a 20-second duration.
- Frame-zero/poster SSIM: 0.997816 after H.264 encoding.
- Audio: 48 kHz stereo AAC, start 0.000000 seconds, duration 20.000000 seconds.
- Audio packet SHA-256 identical before/after poster replacement: f45152dc7b4ce0f4ecd015e42f7cd8806aa6ddcd836d171abd0601072dfd14e7.
- Measured mean volume -25.6 dB and peak -7.2 dB; non-silent and not clipped. Audio inspection was technical (stream/timing/level measurements), not a claim of external listening review.
- Detailed ffprobe reports and validation.json are included.

## Audio and assets

Original procedural 96 BPM music and tap sounds were authored specifically for this film; no samples or third-party recordings. Roboto is covered by its included OFL. GSAP retains its original header, and provenance/license links are in composition/ASSET-RIGHTS.md.

## Delivery and scope

Includes brag.mp4, brag.jpg, brag-plan.md, composition-brief.md, share-copy.txt and reusable composition/. The original render and review evidence remain available locally, with intermediate files excluded from Git. Frame zero is a useful poster, but platform thumbnail selection is not guaranteed. Rendering used no hosted service or paid account, and the temporary preview server was stopped after delivery verification.

After local delivery, Vicky explicitly requested publication on GitHub. The README uses his GitHub attachment link, which he confirmed opens in his browser. An unauthenticated automated request returned HTTP 404, so that check does not establish availability for every viewer. The verified local MP4 is also attached to the v1.0.0 release as a download. The reusable source and delivery documents accompany the video in the repository.
