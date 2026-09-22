# Feature map

## File manager
- SAF folder permission
- Browse folders
- Breadcrumb root
- Search/filter
- Sort by name/date/size/type
- Open files/folders
- Create files/folders
- Rename
- Delete
- Copy/move/duplicate
- Multi-select UI
- ZIP export
- SHA-256
- External open/share capability through Android intent

## Editor
- Text/code editing
- Autosave
- Save / Save As
- JSON formatting/minifying
- HTML preview
- Character/line counters
- Common programming/text extensions
- Default templates for HTML/CSS/JS/JSON/Python

## Projects
- Project creation
- Starter files
- Recent project registry
- Project workspace navigation

## Developer tools
- JSON
- Base64
- URL encode/decode
- Regex tester
- SHA-256
- UUID
- Color picker
- Shell console

## UX
- Dark interface
- Arabic RTL
- Responsive layout
- Activity log
- Persistent settings
- No fake success responses

## Explicit limitations
- Android SAF does not provide a universal recoverable Trash API. The UI records deleted items, but the bridge refuses to claim a restore succeeded.
- Full Git/LSP/Gradle/Maven/CMake integration is not included in this build because those require separate runtimes/toolchains. The shell console can use binaries actually present on the device.
- A browser build cannot provide Android SAF; it falls back to browser APIs.
