# Audiobooks — a Light Phone III tool

Plays DRM-free MP3 audiobooks: one folder per book, one MP3 per chapter,
chapters ordered by filename. Built in the `tool` module of
[lightphone/light-sdk](https://github.com/lightphone/light-sdk)
(SDK current as of October 17, 2026).

## What it does

- Home screen lists your books with resume position ("CH 3 OF 12")
- Player: play/pause, previous/next chapter, skip ±15 seconds, chapter list
- Remembers position per book; auto-advances chapters
- Playback speed (1x / 1.25x / 1.5x / 2x) and sleep timer (15 / 30 / 60 min)
- Loads books over wi-fi: a small upload page served by the tool itself
  (enter the address shown on the LOAD BOOKS screen from a computer on the
  same network; nothing ever leaves the device)
- Background playback: uses the SDK's detached audio player
  (`capabilities = ["detached-audio"]`), so books keep playing with the
  screen off or the tool closed, and system/bluetooth media controls work

## Where the code lives

Everything is in `tool/` — `lighttool.toml` (id, label, permissions,
capabilities) and `tool/src/main/kotlin/com/davidhobbet/audiobooks/`:

- `Library.kt` — scans `files/audiobooks/<Book>/<chapter>.mp3`, natural filename sort
- `PlayerController.kt` — one detached `LightAudioPlayer` for the tool;
  a book is a queue, a chapter is a queue item, usage = Speech
- `ProgressStore.kt` — resume points + speed in the SDK DataStore
- `UploadServer.kt` — Ktor/Netty server on port 8765 (Ktor is allow-listed)
- `HomeScreen.kt`, `PlayerScreen.kt`, `ChapterListScreen.kt`, `UploadScreen.kt`

## Build

Needs a GitHub token with package read access in `local.properties`
(`gpr.user` / `gpr.key`) — see the SDK README. Then:

    ./gradlew :tool:assembleDebug

`lighttool.toml` ships pointed at real LightOS (`serverPackage = "com.lightos"`).
For the LightOS emulator, swap to the commented emulator line and build with
`-DlightSdk.allowAltServerPackage=true`.

## Privacy

No analytics, no tracking, no network calls except the local wi-fi upload
page the user opens deliberately; audio files stay in the tool's private
storage on the device.

## Submission status

Prepared for the Light Tool Library (dashboard → account → developer
account). versionCode 2, versionName 1.1.0.
