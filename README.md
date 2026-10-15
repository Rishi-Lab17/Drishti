# Drishti
Drishti is an AI-powered accessibility assistant for blind and low-vision users, featuring scene descriptions, object detection, OCR text reading, and voice-guided interaction.  Copy description
# Drishti — AI Vision & Voice Assistant

**Making the world more accessible through AI-powered vision and voice guidance.**

Drishti is an Android accessibility application designed to help blind and low-vision users better understand their surroundings. By combining AI-powered image analysis, camera input, text recognition capabilities, and voice interaction, Drishti aims to make everyday visual information more accessible through spoken descriptions and guidance.

## ✨ Key Features

- **AI Scene Understanding** — Analyze visual scenes and generate natural-language descriptions of the surrounding environment.
- **Visual Assistance** — Use the camera to capture and analyze images of objects and surroundings.
- **Text Recognition Assistance** — Help users access text visible in images through text-reading workflows.
- **Voice Interaction** — Use speech input to issue supported commands and ask questions about a scene.
- **Text-to-Speech (TTS)** — Listen to generated descriptions and other supported responses.
- **Camera Controls** — Access camera preview, switch cameras, and use supported camera controls.
- **Scan History** — Review previously saved scan records.
- **Accessible Interface** — An Android interface built with Jetpack Compose, with accessibility-focused controls and visual settings.
- **Haptic Feedback** — Use vibration feedback for supported interactions.
- **User Preferences** — Configure available application and accessibility settings.

## 🎯 Mission

Our mission is to use artificial intelligence to reduce visual information barriers and help people with visual impairments interact more confidently with their surroundings.

Drishti is being developed as an assistive technology project focused on accessibility, independence, and inclusive innovation.

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| Jetpack Compose | Modern declarative user interface |
| CameraX | Camera integration |
| Gemini API | AI-powered image and scene analysis |
| Android SpeechRecognizer | Voice input |
| Android TextToSpeech | Spoken responses |
| Room Database | Local scan history and persistence |
| Kotlin Coroutines and Flow | Asynchronous operations and reactive state |
| OkHttp | HTTP communication with AI services |
| Gradle | Build and dependency management |

## 🏗️ Project Architecture

The application separates the user interface, application state, data management, camera functionality, and audio assistance into dedicated packages.

```text
app/src/main/java/com/example/
├── MainActivity.kt
├── audio/
│   ├── HapticManager.kt
│   ├── TtsManager.kt
│   └── VoiceCommandManager.kt
├── camera/
│   └── CameraPreviewView.kt
├── data/
│   ├── local/
│   │   ├── ScanRecord.kt
│   │   ├── ScanRecordDao.kt
│   │   ├── UserPreferences.kt
│   │   └── VisionVoiceDatabase.kt
│   ├── remote/
│   │   ├── GeminiModels.kt
│   │   └── GeminiVisionService.kt
│   └── repository/
│       └── VisionRepository.kt
└── ui/
    ├── components/
    ├── screens/
    ├── theme/
    └── viewmodel/
        └── VisionViewModel.kt
```

## 🚀 Getting Started

### Prerequisites

- Android Studio with Android SDK support
- JDK compatible with the project's Gradle configuration
- Android device or emulator running Android 7.0 (API 24) or later
- Internet connection for Gemini-powered scene analysis
- A valid Gemini API key for cloud-based AI analysis

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
   ```

2. Open the cloned project in Android Studio.

3. Allow Gradle to synchronize dependencies.

4. Configure the required API key as described below.

5. Connect an Android device with USB debugging enabled, or start an emulator.

6. Build and run the application from Android Studio.

### Configure the Gemini API Key

Drishti supports Gemini-powered image analysis. A valid API key is needed to use the cloud-based analysis service.

1. Obtain an API key through [Google AI Studio](https://aistudio.google.com/).
2. Configure the key through the project's supported Gradle Secrets Plugin configuration.
3. Keep your actual key in a local, untracked configuration file and verify the project's `.gitignore` rules before committing.
4. Never commit API keys, passwords, signing credentials, or other secrets to GitHub.

The repository includes an `.env.example` file to document the expected configuration. It contains a placeholder, not a real API key.

**Security note:** An API key embedded in an Android application can potentially be extracted. For a production release, consider routing sensitive API calls through a secured backend with authentication, rate limiting, and usage controls.

If a Gemini API key is not configured, the current implementation includes a fallback for basic on-device frame assessment. This fallback should not be assumed to provide the same capabilities or accuracy as Gemini-powered analysis.

## 🔐 Permissions

Drishti requests Android permissions for the following features:

- **Camera:** Capture and analyze visual information.
- **Microphone:** Accept voice commands.
- **Internet:** Communicate with online AI services.
- **Vibration:** Provide supported haptic feedback.

Grant only the permissions needed for the features you intend to use.

## 🧪 Testing

The project contains unit and instrumentation test sources.

Run unit tests on Windows:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run Android instrumentation tests on a connected device or emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Run a debug build:

```powershell
.\gradlew.bat assembleDebug
```

These commands assume that the Android SDK and required build tools are installed and configured correctly.

## ♿ Accessibility and Responsible Use

Drishti is designed to support accessibility, not to replace a mobility aid, trained orientation and mobility support, or a trusted human assistant.

AI-generated descriptions can be incomplete, delayed, or incorrect. Users should independently verify safety-critical information. Do not rely on the application alone to detect hazards, navigate roads, or make emergency decisions.

## 🗺️ Roadmap

Potential areas for future development include:

- Improved real-time scene understanding
- More reliable text reading and OCR workflows
- Expanded language and speech support
- Better low-vision accessibility options
- Faster voice-command processing
- Enhanced scan history and organization
- Improved testing, performance, and production security
- More robust navigation and obstacle-awareness assistance

Roadmap items are planned improvements and should not be interpreted as already implemented features.

## 🤝 Contributing

Contributions, bug reports, accessibility feedback, and feature suggestions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Make your changes and test them.
4. Open a pull request describing the changes.

Please avoid including personal data, API keys, or other secrets in issues and pull requests.

## 🔒 Privacy

Camera images and voice input may contain sensitive personal information. Cloud-based image analysis may transmit images to an external AI service. Review the applicable service terms and privacy policies before using these features with sensitive information.

Before a public release, document data retention, network transmission, and deletion behavior clearly.

## 📄 License

No license has been specified yet. Until a license is added to the repository, reuse and redistribution are subject to applicable copyright law.

## 💙 Built for Accessibility

Drishti is an accessibility-focused AI project exploring how computer vision and voice technologies can help make visual information more accessible to everyone.
