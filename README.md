# ⚽ Gridline Soccer

> A digital reimagining of the classic paper soccer game with online multiplayer, tournaments, and AI opponents.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)

## 📖 About

Gridline Soccer brings the nostalgic pen-and-paper soccer game to your mobile device. Players sketch passes on a grid, bounce off sidelines for creative angles, and aim to score goals. The game features same-device play, online friend matches, AI opponents, and competitive tournaments with global leaderboards.

**Based on:** The classic [paper soccer](https://en.wikipedia.org/wiki/Paper_soccer) game (also known as "paper hockey" or "pen soccer")

## ✨ Features

### 🎮 Game Modes
- **Local Multiplayer** - Play against friends on the same device
- **Online Matches** - Challenge friends remotely with real-time gameplay
- **AI Opponent** - Practice against intelligent AI with adjustable difficulty
- **Tournament System** - Compete in brackets with rankings and leaderboards

### 🌍 Global & Regional Support
- **20+ Languages** - Full localization support
- **Regional Variants** - Global and Bangladesh-specific versions
- **Compliance Features** - Age ratings and regional regulations support

### 🛡️ Safety & Moderation
- **AI-Powered Filtering** - Vertex AI content moderation for nicknames
- **User Management** - Account verification and security features
- **Community Standards** - Automated content monitoring

### 📊 Engagement & Analytics
- **Firebase Analytics** - Comprehensive event tracking
- **Remote Config** - A/B testing and feature flags
- **Push Notifications** - Match invites and tournament updates
- **Crash Reporting** - Automatic error tracking with Crashlytics

## 🏗️ Architecture

### Technology Stack

**Mobile App (Android)**
- **Language:** Java/Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36
- **Build System:** Gradle
- **Key Libraries:**
  - AndroidX (AppCompat, ConstraintLayout, WorkManager)
  - Firebase SDK (Auth, Firestore, Realtime DB, Messaging, Analytics, Crashlytics)
  - Google Sign-In, Facebook SDK
  - Glide (Image Loading), OkHttp (Networking)

**Backend (Firebase)**
- **Database:** Cloud Firestore (primary) + Realtime Database (game state sync)
- **Functions:** 24 Cloud Functions (Node.js)
- **AI/ML:** Vertex AI (Gemini) for content moderation
- **Infrastructure:** Google Cloud Platform
- **Security:** Firestore Rules, Authentication

### Project Structure

```
Soccer/
├── mobile/              # Android application
│   ├── app/            # Main app module
│   │   ├── src/
│   │   │   ├── main/   # Source code and resources
│   │   │   └── test/   # Unit and integration tests
│   │   └── build.gradle
│   └── build.gradle
├── firebase/           # Backend infrastructure
│   ├── functions/      # Cloud Functions (24 services)
│   ├── firestore.rules # Database security rules
│   ├── firestore.indexes.json
│   └── seed/           # Initial data (regulations, configs)
├── gcp/               # Google Cloud Platform configs
├── tools/             # Utility scripts
│   ├── create-tournament/
│   ├── check-translation-completeness.py
│   └── ...
├── docs/              # Technical documentation (60+ docs)
├── firebase-hosting/  # Web hosting assets
└── animation/         # UI animations and assets
```

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Arctic Fox or later)
- **JDK 11** or higher
- **Node.js 18+** (for Firebase Functions)
- **Firebase CLI** (`npm install -g firebase-tools`)
- **Python 3** (for utility scripts)

### Initial Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/piotr-gorczynski/Soccer.git
   cd Soccer
   ```

2. **Install dependencies**
   ```bash
   npm install
   cd mobile && ./gradlew build
   ```

3. **Configure Firebase**
   
   Create a `secrets/` directory in the project root and add Firebase configuration files:
   ```
   secrets/
   ├── google-services-dev.json
   ├── google-services-test.json
   ├── google-services-prod.json
   └── keystore.properties (for release builds)
   ```
   
   See [`docs/FACEBOOK_SETUP.md`](docs/FACEBOOK_SETUP.md) for authentication setup.

4. **Build the app**
   ```bash
   cd mobile
   ./gradlew assemble_devGlobalDebug
   ```

### Build Variants

The app supports multiple build configurations:

| Environment | Market | Description |
|------------|--------|-------------|
| `dev` | Global | Development environment with test data |
| `test` | Global | Staging environment for QA |
| `prod` | Global | Production (global markets) |
| `prod` | Bangladesh | Production (Bangladesh-specific) |

**Example builds:**
```bash
# Development build (Global)
./gradlew assemble_devGlobalDebug

# Production release (Global)
./gradlew assemble_prodGlobalRelease

# Production release (Bangladesh)
./gradlew assemble_prodBangladeshRelease
```

## 🔧 Development

### Running Tests

```bash
cd mobile
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumentation tests
```

### Firebase Functions

Deploy Cloud Functions:
```bash
cd firebase
firebase deploy --only functions
```

Deploy specific function:
```bash
firebase deploy --only functions:create-invite
```

### Translation Validation

Ensure all languages have complete translations:
```bash
python3 tools/check-translation-completeness.py
```

This runs automatically in CI/CD for translation-related PRs.

### Creating Tournaments

Use the helper script to create tournaments:
```bash
node tools/create-tournament/create-tournament.js dev "Summer Cup" 16 \
  "2024-06-01T12:00:00Z" "2024-07-01T12:00:00Z" "regDocId"
```

Or use a JSON configuration file:
```bash
node tools/create-tournament/create-tournament.js dev params.json
```

### Release Signing

For release builds, create `secrets/keystore.properties`:
```properties
KEYSTORE_FILE=secrets/keystore.jks
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

**Note:** The `secrets/` directory is excluded from version control.

### Facebook Integration

Generate Facebook key hashes for authentication:
```bash
cd mobile
./gradlew generateFacebookKeyHashes
```

Then add the generated hashes to your Facebook App Dashboard. See [`docs/FACEBOOK_SETUP.md`](docs/FACEBOOK_SETUP.md) for detailed instructions.

## 📚 Documentation

Comprehensive documentation is available in the [`docs/`](docs/) directory:

- **Setup Guides:** Facebook setup, Firebase configuration, environment setup
- **Architecture:** Backend design, database schema, Cloud Functions
- **Bug Fixes:** 50+ detailed fix analyses and solutions
- **CI/CD:** Deployment pipelines, automation strategies
- **Testing:** QA procedures, consent testing, analytics validation

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes following the existing code style
4. Run tests and ensure they pass
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Code Quality

- Follow Android best practices and Material Design guidelines
- Write unit tests for new features
- Ensure translation completeness for UI changes
- Document complex logic and architectural decisions

## 📋 Recent Improvements

### Facebook Login Fix
Fixed "Invalid key hash" errors in release builds. Run `./gradlew generateFacebookKeyHashes` and add hashes to Facebook App Dashboard.

### Profile Photo Fix
Resolved blank profile photo issues for Facebook users by using Graph API with access tokens instead of generic picture URLs.

### AI Content Moderation
Integrated Vertex AI (Gemini) for automated nickname filtering and content safety.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**Copyright © 2018-2026 Piotr Gorczynski**

## 🔗 Links

- [Firebase Console](https://console.firebase.google.com/)
- [Google Play Console](https://play.google.com/console/)
- [Documentation](docs/)

---

**Built with ❤️ using Firebase and Android**
