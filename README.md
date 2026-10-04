# Decibel — offline / online YouTube music player

Android app built with Jetpack Compose and Material 3.

## Features

- Browse Trending music + Search (scrollable YouTube lists)
- Preview → **Play online** (audio) or **Download** progressive video for offline
- Music-player UX for online streams and offline MP4
- Background playback (`PlaybackService` + MediaSession notification)
- Background downloads (`DownloadForegroundService`)
- Bottom dock: Browse | Library | Playing | Settings

## Build

```bash
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Notes

- Personal offline / listening use; respect creators and YouTube’s terms.
- No MP3 conversion — downloads are progressive video files played as audio.
