# Quick File Studio

Android file/project manager built with HTML5 + CSS3 + JavaScript and a native Android SAF bridge.

## Implemented
- Android Storage Access Framework folder access
- File/folder browser with breadcrumbs, search, sort, multi-select
- Create, rename, delete, move/copy, duplicate
- Trash with restore / permanent delete
- Text/code editor with autosave, line numbers, syntax-aware status
- Create HTML/CSS/JS/JSON/Python/Markdown/text files
- Project workspace detection and project templates
- Project tree and recent projects
- Global content search for text files
- Preview HTML files inside the app
- Image/audio/video/document external opening
- ZIP create/extract
- File hashing (SHA-256)
- JSON formatter/minifier, Base64, URL encode/decode, UUID, regex tester, color tools
- Storage statistics
- Command console through Android shell (only commands available on the device)
- Import/export using Android document picker
- Share/open with Android
- Dark/light themes, Arabic/English UI, settings persistence
- Activity log
- Keyboard shortcuts where supported
- Web fallback using browser File System Access API / file input

## Important
The Android bridge performs real operations through SAF. Features that depend on Android/device capabilities are disabled when the bridge is unavailable rather than pretending to work.

## Build
Requirements:
- JDK 17+
- Android SDK with platform 35
- Build tools 35.0.0
- Gradle 8.9+ (or your installed Gradle 9.x)

From the project root:
```bash
gradle --no-daemon assembleDebug
```

APK:
`android/app/build/outputs/apk/debug/app-debug.apk`

## First run
Open the app and press "Choose folder". Grant access to a workspace folder. The app then uses that folder for real file operations.
