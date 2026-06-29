# Floppy Android MVP

Floppy is a 7-day Android MVP for the bedtime audio loop: profile intake, recommendation or generation, playback, feedback, and settings.

## Stack

- Kotlin, Jetpack Compose, ViewModel, Coroutines/Flow
- Mock-first repository with Retrofit/OkHttp API contracts ready for backend integration
- Media3 ExoPlayer playback adapter
- Minimum SDK 29, target SDK 36

## Build

This workspace keeps Gradle writable state in the project to work with the local sandbox:

```bash
GRADLE_USER_HOME=.gradle-home gradle test assembleDebug
```

Debug builds use mock data by default. Release builds set `BuildConfig.USE_MOCK_API=false` and expect the backend base URL in `BuildConfig.API_BASE_URL`.
