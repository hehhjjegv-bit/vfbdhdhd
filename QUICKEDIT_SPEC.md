# Quick File Studio - Editor Specification

## Core
- File manager
- Android SAF
- File open/save/save as
- Create file/folder
- Rename/delete/copy/move
- Multiple editor tabs
- Undo/redo
- Search/replace
- Goto line
- Statistics
- Syntax/language selection
- Encoding
- Themes
- Word wrap
- Read only
- Mini toolbar
- Share
- Preview/execute where supported
- Recent files
- Bookmarks
- Settings
- Keyboard shortcuts
- Arabic/English interface and extensible localization

## Important
Every visible button must have a real implementation.
No fake buttons.
No proprietary QuickEdit code, assets, or branding.
Implement functionality independently.

## Storage
Use Android Storage Access Framework.
Do not claim unrestricted access to protected Android/system directories.

## Editor languages
Support file associations and syntax modes progressively.
Do not claim 170+ implemented languages until the implementation actually contains them.
