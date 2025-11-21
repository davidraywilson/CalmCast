# CalmCast
Mindful podcast streaming on Android. A thoughtfully designed podcast app built with Kotlin and Jetpack Compose, adhering to the **Mudita Mindful Design framework**. Intentional, distraction-free discovery and listening without tracking, algorithmic manipulation, or feature bloat.

## Features
- **Subscriptions Screen**: Browse your curated podcast collection in a focused, organized view
- **Podcast Discovery**: Real-time search to find podcasts without algorithmic interference
- **Episode Details**: View full episode archives, descriptions, and publication history
- **Offline Listening**: Download episodes for reliable offline playback
- **Pause & Resume**: Download management with resume capability
- **Privacy-First**: No tracking, no data collection, no account required

## Tech Stack
- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose
- **Design System**: Material Design 3
- **Navigation**: Jetpack Navigation Compose
- **Architecture**: MVVM + StateFlow
- **Database**: Room
- **Download Manager**: AndroidDownloadManager (with resume)
- **Min SDK**: 24 | **Target SDK**: 34

## Project Structure
```
CalmCast/
├── app/
│   ├── src/main/java/com/calmcast/podcast/
│   │   ├── MainActivity.kt
│   │   ├── CalmCastApplication.kt
│   │   ├── PlaybackService.kt
│   │   ├── data/
│   │   │   ├── Models.kt
│   │   │   ├── PodcastDatabase.kt
│   │   │   ├── dao/
│   │   │   └── download/
│   │   └── ui/
│   │       ├── PodcastViewModel.kt
│   │       ├── Screens.kt
│   │       └── components/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── DOWNLOAD_BEHAVIOR.md
```

## Getting Started
### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11+
- Android SDK 34+
- Device or emulator running API 24+
### Setup
1. Clone and open: `git clone https://github.com/yourusername/CalmCast.git && cd CalmCast`
2. Open in Android Studio
3. Build: Build → Make Project
4. Connect device or start emulator
5. Run: Run → Run 'app' (Shift + F10)
### First Launch
CalmCast includes sample podcasts. Explore by browsing subscriptions, searching for new podcasts, subscribing, and downloading episodes for offline access.

## Usage
### Subscriptions Screen
View all subscribed podcasts in a focused, organized library. Tap any podcast to explore episodes.
### Podcast Detail
- View title, author, description
- Browse full episode archive
- Download episodes for offline access
- Subscribe/unsubscribe
### Search Screen
Discover new podcasts by title, author, or keywords without algorithmic interference.
### Download Management
- Start downloads with one tap
- Pause and resume downloads anytime
- Delete downloaded episodes to free space
- Enjoy offline playback without internet

## Mudita Mindful Design Principles
- **Simplicity**: Single, clear purpose per screen. No bloat, no decision paralysis
- **Privacy**: No tracking, no algorithmic manipulation, no data monetization
- **Intention**: Features support deliberate choices, not engagement traps
- **Focus**: Clean interface that respects your attention
- **Offline-First**: Downloads built-in, reducing dependency on connectivity

## Privacy & Data
- No account required
- No data collection or transmission
- No tracking or analytics
- All preferences stored locally
- Offline-first architecture
### Download Behavior
- Resume support for interrupted downloads
- Reliable background download management
- Automatic cleanup of corrupted records
- See `DOWNLOAD_BEHAVIOR.md` for details

## Future Enhancements
- Integration with podcast APIs (Listen Notes, PodcastIndex)
- Enhanced media playback controls
- Sleep timer for mindful listening
- Bookmarking and notes
- Accessibility improvements
- Optional privacy-respecting cloud sync
## Contributing
Contributions welcome! Please align changes with core Mudita Mindful Design principles: simplicity, privacy, and focus. Test thoroughly on target Android versions.
## License
GNU General Public License v3 (GPLv3). See LICENSE for details.
## Philosophy
"More offline. More life." — CalmCast is built on the belief that technology should serve you, not demand your attention. By embracing simplicity, privacy, and intention, we've created a podcast experience that respects your time and supports mindful consumption.
