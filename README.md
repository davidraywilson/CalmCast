<div align="center">
  <img src="logo.svg" width="100" height="100" alt="CalmCast Logo">
</div>

# CalmCast

CalmCast is a minimal mindful podcast app built to work on de-googled E-ink devices utilizing the Mudita Mindful Design library.

"Let's make technology useful again."

## Screenshots

<table>
<tr>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170448.png"></td>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170457.png"></td>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170508.png"></td>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170521.png"></td>
</tr>
<tr>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170525.png"></td>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170555.png"></td>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170601.png"></td>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170611.png"></td>
</tr>
<tr>
<td><img src="CalmCast%20Screens/Screenshot_20251121-170615.png"></td>
</tr>
</table>

## What is CalmCast?
CalmCast keeps podcasting calm. It’s thoughtfully designed with Kotlin and Jetpack Compose to prioritize your attention, privacy, and time. No accounts, no analytics, no dark patterns—just your shows, your choices.

## Core principles (Mudita Mindful Design)
- Simplicity: One clear purpose per screen; no feature bloat
- Privacy: No tracking, no data monetization, no third‑party analytics
- Intention: Tools that support deliberate listening, not habit loops
- Focus: Clean UI that stays out of your way
- Offline‑first: Robust downloads reduce dependency on connectivity

## Highlights
- Browse and subscribe to shows in a focused library
- Thoughtful discovery via search (no algorithmic manipulation)
- Complete episode archives with rich details
- Offline listening with pause/resume downloads
- Download management to free space and stay organized

## Why it matters
- Privacy‑first: Everything stays on your device; preferences are stored locally
- Distraction‑free: No ads, no pushy prompts, no engagement targets
- Reliable: Resume interrupted downloads and listen without network access

## Tech stack (for the curious)
- Language: Kotlin 1.9+
- UI: Jetpack Compose + Material Design 3
- Navigation: Jetpack Navigation Compose
- Architecture: MVVM + StateFlow
- Persistence: Room
- Downloads: AndroidDownloadManager (with resume)
- Android: Min SDK 24, Target SDK 34

## Privacy & data
- No account required
- No data collection or transmission
- No tracking or analytics
- All preferences are stored locally on‑device

### Download behavior
- Resume support for interrupted downloads
- Reliable background download management
- Automatic cleanup of corrupted records

## Roadmap
- Sleep timer for mindful listening
- Bookmarking
- Accessibility improvements
- Additional api sources for user preference

## For developers
Want to build from source?
- Requirements: Android Studio (Arctic Fox or newer), JDK 11+, Android SDK 34+, device/emulator API 24+
- Quick start: clone → open in Android Studio → Build → Run

## Contributing
Contributions are welcome—please align with the core principles of simplicity, privacy, and focus. Test on supported Android versions.

## License
GPL‑3.0 (see `LICENSE`).
