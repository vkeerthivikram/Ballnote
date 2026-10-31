# Ballnote launch composition

This is a self-contained, locally rendered 20-second launch film. The editable scene HTML is in `compositions/`; the root is `index.html`. Assets are local. The Android project is not modified.

## Reproduce on Windows

Requires Node 22+, Chrome, and the pinned dependencies in package-lock.json:

```powershell
npm ci
node synthesize-audio.mjs
.\run-hf.ps1 check --json
.\run-hf.ps1 preview --background --no-open --port 3127
.\run-hf.ps1 render --quality delivery --workers 2 --output ../brag-original.mp4
node finish.mjs
.\run-hf.ps1 preview --stop
```

`run-hf.ps1` selects the project-local FFmpeg/ffprobe binaries and disables optional telemetry with DO_NOT_TRACK. For other platforms, set the equivalent HYPERFRAMES_FFMPEG_PATH, HYPERFRAMES_FFPROBE_PATH and HYPERFRAMES_BROWSER_PATH values for your installation.

Edit HTML and CSS directly to refine the composition. `generate.mjs` regenerates the initial scene files and overwrites them: run it only when intentionally rebuilding from that source.

`finish.mjs` extracts the settled hero frame at 4.8 seconds as brag.jpg, replaces only video frame zero with it, and validates 1920×1080, 30 fps, 600 frames, 20 seconds, unchanged AAC audio packets and matching audio timing. The original render is kept as brag-original.mp4.

The screenshots are real native app captures with fictional demo teams. Highlights are editorial emphasis over still images, not a claimed recording of live interaction. See ASSET-RIGHTS.md for provenance and audio/font rights.
