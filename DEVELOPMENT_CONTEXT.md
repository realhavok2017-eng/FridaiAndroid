# FRIDAI Android - Development Context

## Last Session: December 24, 2024 (4:15 AM)

### What FRIDAI Is
FRIDAI (Female Responsive Intelligent Digital AI Interface) is a Tony Stark-style AI assistant. She has her own personality, runs on a Flask backend at `https://fridai.fridai.me`, and this Android app is her mobile interface.

### Current State: WORKING
The app is fully functional with:
- **Wake word detection**: "Hey Friday" using Picovoice Porcupine v4.0.0
- **Floating overlay**: Pops up like Google Assistant when wake word detected
- **Conversational**: Overlay stays open for back-and-forth chat
- **Voice flow**: Record → Transcribe → Chat with Claude → TTS → Speak response
- **Persistence**: Wake word setting survives app restarts and device reboots

### Key Files

| File | Purpose |
|------|---------|
| `FridaiOverlayService.kt` | Floating overlay popup with full conversation support |
| `AlwaysListeningService.kt` | Background wake word detection service |
| `WakeWordDetector.kt` | Porcupine integration for "Hey Friday" |
| `SettingsScreen.kt` | Settings UI with permissions management |
| `BootReceiver.kt` | Auto-starts wake word on device boot |
| `FridaiAvatar.kt` | Animated avatar (Compose) |
| `FridaiAvatarView.kt` | Avatar for non-Compose contexts |

### Backend API (Flask)
- `POST /transcribe` - Audio → Text (expects base64 JSON, NOT multipart)
- `POST /chat` - Text → Claude response
- `POST /speak` - Text → TTS audio (base64 MP3)
- `GET /health` - Connection check

### Known Issues / TODO
1. **Backend**: FRIDAI responds "Done." to repeated goodbyes - tweak system prompt
2. **Future**: Add Android intents for alarms, calendar, etc.

### Technical Notes
- Android 14+ requires foreground service to start from Activity (not Application)
- Wake word service uses `FOREGROUND_SERVICE_MICROPHONE` type
- Overlay uses `SYSTEM_ALERT_WINDOW` permission
- Audio recording uses ShortArray with little-endian byte conversion for proper WAV

### How to Continue
1. Open the app project in Android Studio or Claude Code
2. Read this file for context
3. Check `git log` for recent changes
4. The app is stable - test with "Hey Friday" before making changes
