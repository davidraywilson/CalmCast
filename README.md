# CalmCast - Mindful Podcast Streaming

A thoughtfully designed Android podcast app built with Kotlin and Jetpack Compose, adhering to the **Mudita Mindful Design framework**. CalmCast brings intentional, distraction-free podcast discovery and streaming to your Android device, helping you cultivate a more mindful relationship with content consumption.

## 🧘 Mindfulness First

At its core, CalmCast embraces the principles of Mudita Mindful Design, a framework for building clear, intentional interfaces grounded in simplicity, privacy, and intentional use. This means:

- **Simplicity Over Complexity**: CalmCast strips away unnecessary features and focuses on what matters—discovering, subscribing to, and listening to podcasts with intention
- **Privacy by Design**: Your podcast preferences and listening history are private by default. No tracking, no data monetization
- **Focus-First Interface**: A clean, distraction-free UI that respects your attention and doesn't demand constant interaction
- **Offline-Ready**: Download episodes to enjoy content without unnecessary connectivity demands
- **Intentional Design**: Every feature serves a clear purpose, supporting healthier tech habits

## 🎯 Core Features

**Subscriptions Screen**
Browse your carefully curated podcast collection. Only the podcasts that matter to you, organized simply and cleanly.

**Podcast Discovery**
Search for new podcasts with a real-time search interface that helps you find exactly what you're looking for, without algorithmic manipulation or recommendation fatigue.

**Detailed Episode Information**
When you find a podcast, explore its episodes, descriptions, and publication history. Make informed decisions about what to listen to.

**Offline Listening**
Download episodes for offline playback. Take your podcasts with you without relying on constant connectivity or streaming data usage.

## 🛠 Tech Stack

Built with modern Android best practices:

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material Design 3
- **Navigation**: Jetpack Navigation Compose
- **Architecture**: MVVM with ViewModel
- **Database**: Room (local persistence)
- **Download Management**: AndroidDownloadManager with resume capability
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## 📁 Project Structure

```
CalmCast/
├── app/
│   ├── src/main/
│   │   ├── java/com/calmcast/podcast/
│   │   │   ├── MainActivity.kt              # App entry point
│   │   │   ├── CalmCastApplication.kt       # Application class
│   │   │   ├── PlaybackService.kt           # Media playback service
│   │   │   ├── data/
│   │   │   │   ├── Models.kt                # Data classes for Podcasts, Episodes
│   │   │   │   ├── PodcastDatabase.kt       # Room database configuration
│   │   │   │   ├── dao/                     # Data Access Objects
│   │   │   │   └── download/                # Download management
│   │   │   └── ui/
│   │   │       ├── PodcastViewModel.kt      # State management
│   │   │       ├── Screens.kt               # Compose UI screens
│   │   │       └── components/              # Reusable Compose components
│   │   ├── AndroidManifest.xml
│   │   └── res/                             # Resources
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── DOWNLOAD_BEHAVIOR.md                     # Download implementation details
```

## 🚀 Getting Started

### Prerequisites

- Android Studio (Arctic Fox or later)
- JDK 11 or higher
- Android SDK 34+
- Android device or emulator running API 24+

### Setup Instructions

1. **Clone and Open**
   ```
   git clone https://github.com/yourusername/CalmCast.git
   cd CalmCast
   ```

2. **Open in Android Studio**
   - File → Open → Select the CalmCast folder
   - Android Studio will automatically sync Gradle files

3. **Build the Project**
   - Build → Make Project
   - Wait for the build to complete

4. **Run the App**
   - Connect an Android device via USB or start an emulator
   - Run → Run 'app' (or press Shift + F10)
   - Select your target device

### First Launch

CalmCast comes with a selection of thoughtfully chosen sample podcasts to explore. Use the app to:
- Browse available podcasts on the Subscriptions screen
- Search for new podcasts using the Search screen
- Subscribe to podcasts you want to follow
- Download episodes for offline listening
- Enjoy calm, intentional podcast consumption

## 📱 Using CalmCast

### Subscriptions Screen
Your personal podcast library. All your subscribed podcasts in one focused view. Tap any podcast to explore its episodes.

### Podcast Detail Screen
- View podcast title, author, and description
- See the full episode archive
- Download episodes for offline access
- Subscribe or unsubscribe with a single tap

### Search Screen
Discover new podcasts by title, author, or keywords. Real-time search results help you find exactly what you're looking for without algorithmic interference.

### Download Management
- **Start Download**: Tap the download icon to save an episode locally
- **Pause/Resume**: Pause downloads to save bandwidth; resume anytime to continue from where you left off
- **Delete**: Remove downloaded files to free up space
- **Offline Playback**: Download episodes and enjoy them without internet connectivity

## 🧠 Mudita Mindful Design Principles

CalmCast embodies these core design principles:

**Simplicity**
Every screen has a single, clear purpose. No feature bloat, no decision paralysis. You decide what podcasts matter to you.

**Privacy**
No tracking, no algorithmic recommendations designed to keep you engaged longer, no data monetization. Your podcast preferences are yours alone.

**Intention**
Features are designed to support deliberate choices. Search for podcasts, subscribe consciously, choose what to download. No auto-play traps or infinite scroll.

**Focus**
The interface gets out of your way. A clean, minimal design that respects your attention and supports deep listening.

**Offline-First Thinking**
Downloads are built in, not an afterthought. Reduce your dependency on constant connectivity and data usage.

## 🔒 Privacy & Data

CalmCast is designed with privacy as a core principle:
- No account required
- No data collection or transmission
- No advertisement or tracking
- All podcast preferences stored locally on your device
- No third-party analytics

## 📥 Download Behavior

CalmCast provides robust download management:
- **Resume Support**: Pause downloads and resume from exactly where you left off
- **Offline Access**: Download episodes for reliable offline playback
- **Space Management**: Delete downloaded episodes anytime to free up device storage
- **Background Downloads**: Downloads continue reliably in the background
- **Smart Cleanup**: Corrupted download records are automatically cleaned on app startup

See `DOWNLOAD_BEHAVIOR.md` for detailed technical documentation.

## 🌱 Future Enhancements

As CalmCast evolves, future enhancements may include:
- Integration with podcast APIs (Listen Notes, PodcastIndex)
- Local database persistence for robust caching
- Enhanced media playback controls
- Sleep timer for mindful listening
- Bookmarking and notes on episodes
- Calm, accessibility-focused design improvements
- Optional cloud synchronization (privacy-respecting)

We'll always prioritize simplicity and mindfulness over feature complexity.

## 🤝 Contributing

Contributions are welcome! Whether it's bug reports, feature suggestions, or code improvements, please consider how changes align with our core principle: **simplicity and mindfulness first**.

When contributing:
1. Ensure your changes support the core Mudita Mindful Design principles
2. Keep the interface simple and focused
3. Maintain privacy-by-design practices
4. Test thoroughly on target Android versions

## 📄 License

CalmCast is open source and available under the GNU General Public License v3 (GPLv3). See LICENSE file for details.

## 🙏 Philosophy

> "More offline. More life."

CalmCast is built with the belief that technology should serve you, not demand your attention. In an age of information overload, podcast apps often compete for your engagement with algorithmic recommendations and infinite scroll. CalmCast takes a different approach.

By embracing the Mudita Mindful Design framework, we've created a podcast app that:
- Respects your time and attention
- Prioritizes your privacy
- Supports intentional listening
- Reduces digital clutter
- Encourages mindful consumption

Whether you're exploring new ideas, enjoying storytelling, or learning something new, CalmCast is designed to enhance your podcast experience without hijacking your attention or compromising your privacy.

Enjoy calm, intentional listening.

---

**Have questions or ideas?** Open an issue on GitHub or contribute to the conversation about mindful technology in podcast consumption.
