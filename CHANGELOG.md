# Changelog

All notable changes to this project will be documented in this file.

## [3.5.19] - 2026-03-01

### Added

- **Adhan Event Tracking**: New "Past Adhan Events" screen to track the full lifecycle of Adhan playback.
  - Logs `Scheduled`, `Triggered`, `Playing`, and `Success/Error` events.
  - Captures speaker/audio route and volume level for each playback.
  - Includes date and time for precise diagnostic history.
- **Improved Countdown**: Dashboard now shows a live hours/minutes countdown for the next prayer, including "Fajr Tomorrow" when all of today's prayers are finished.
- **Enhanced README**: Added comprehensive documentation on features, architecture, technology stack, and credits.
- **Sister Project Link**: Added a direct link to the iOS version of the project.

### Fixed

- **Log Copy Crash**: Fixed a critical crash in the `LogsScreen` when copying logs to the clipboard by limiting the buffer size.
- **Log Management**: Implemented `LogRepository` size limiting (500KB) and automatic rotation to prevent memory issues.

### Changed

- Updated `AdhanService` and `AlarmReceiver` to provide more granular logging during the playback lifecycle.
- Refined UI layout in `SettingsScreen` and `PastEventsScreen` for better readability and diagnostics.

---

*Based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).*
