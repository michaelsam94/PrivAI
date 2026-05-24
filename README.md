# PrivAI

**Secure notes, voice transcription, and document OCR — 100% on-device.**

PrivAI is an Android workspace for capturing ideas, transcribing speech, and extracting text from photos. Core processing runs locally on your phone. No account required. No cloud uploads for your content.

## Features

- **Workspace notes** — Create, tag, search, and open detailed note views. Everything stays in a local Room database.
- **Voice transcription** — Record and transcribe speech using the device speech recognizer, processed on-device.
- **Document OCR** — Pick a photo from the system image picker and extract text offline with ML Kit (fast mode) or Tesseract (high-accuracy mode). English and Arabic tessdata are bundled.
- **On-device summaries** — Bullet points, keyword highlights, and simple sentiment hints from local NLP — no API keys needed for basic intelligence.
- **Unified dashboard** — Switch between Notes, Audio Transcripts, and OCR Extracts from one home screen.

## Privacy

| Data | Handling |
|------|----------|
| Notes & transcripts | Stored locally in Room |
| OCR input images | Read only when you pick one; briefly cached in app storage for processing |
| OCR output | Extracted text saved locally; images are not uploaded |
| Summaries | Computed on-device via `LocalTextIntelligence` |
| Accounts / cloud sync | Not used |

## Tech stack

- **UI** — Jetpack Compose, Material 3, Navigation Compose
- **Architecture** — ViewModel, Kotlin Coroutines, StateFlow
- **Storage** — Room, DataStore (OCR preferences)
- **Speech** — Android `SpeechRecognizer` via `AndroidSpeechTranscriber`
- **OCR** — ML Kit Text Recognition, Tesseract4Android
- **Testing** — JUnit, Robolectric, Roborazzi (Play Store screenshots)

## Requirements

- Android Studio (recent stable; project uses AGP 9.x)
- JDK 11+
- Android SDK with `compileSdk` 36
- Device or emulator on **API 24+** (Android 7.0)

## Getting started

1. Clone the repository and open the **PrivAI** folder in Android Studio.
2. Let Gradle sync finish (Android Studio will download dependencies).
3. Connect a device or start an emulator.
4. Run the **app** configuration (debug signing uses `debug.keystore` in the project root).

No API keys are required for core features. A `.env.example` file exists for optional secrets-plugin wiring but is not needed for notes, OCR, transcription, or summaries.

## Build commands

Use Android Studio **Build → Build Bundle(s) / APK(s)** or Gradle from the project root:

```bash
# Debug APK
./gradlew :app:assembleDebug

# Release App Bundle (Play Store)
./gradlew :app:bundleRelease
```

Release output: `app/build/outputs/bundle/release/app-release.aab`

### Release signing

Release builds expect a upload keystore. Configure via environment variables or defaults in `app/build.gradle.kts`:

| Variable | Description |
|----------|-------------|
| `KEYSTORE_PATH` | Path to `.jks` file (default: `my-upload-key.jks` in project root) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password (alias: `upload`) |

**Do not commit signing keys to version control.** Add keystore files to `.gitignore` and keep passwords in CI secrets or local env only.

## Permissions

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Live voice transcription |
| `INTERNET` | Required by some on-device ML libraries; user content is not uploaded |

OCR uses the system photo picker (`GetContent`); no storage or media-library permissions are declared.

## Play Store assets

Listing copy, icons, feature graphic, and phone/tablet screenshots live in [`play-store/`](play-store/). Regenerate graphics:

```bash
bash ~/.cursor/skills/generate-app-assets/scripts/generate-app-icon.sh .
./gradlew generatePlayStoreAssets
bash ~/.cursor/skills/generate-app-assets/scripts/verify-play-store-assets.sh .
```

See [`play-store/README.md`](play-store/README.md) for asset sizes and paths.

## Project structure

```
app/src/main/java/com/michael/privai/
├── MainActivity.kt              # Navigation shell
├── data/                        # Room entities, DAOs, repositories, preferences
├── domain/
│   ├── scanner/                 # OCR engines, speech transcriber, image prep
│   └── summarizer/              # LocalTextIntelligence (on-device NLP)
└── ui/
    ├── screens/                 # Home, OCR, transcription, note detail
    ├── theme/
    └── viewmodel/               # PrivAIViewModel

app/src/main/assets/tessdata/    # Bundled Tesseract language models (eng, ara)
play-store/                      # Store listing assets and descriptions
```

## Tests

```bash
# Unit tests (excludes Play Store screenshot tests)
./gradlew :app:testDebugUnitTest

# Play Store screenshot generation (Roborazzi)
./gradlew generatePlayStoreAssets
```

## License

See repository license terms. If no `LICENSE` file is present, all rights reserved by the project owner.
