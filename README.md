# JARVIS Android

An original native Kotlin Android assistant prototype. Its command handling and interface are written specifically for this repository; no source code is copied from other assistant projects.

## Current phone-side features

- Enumerates launchable apps visible to Android's package manager and displays their installed icons.
- Searches and launches apps using normalized names and conservative fuzzy matching.
- Accepts typed commands and speech-recognition results from the device's selected Android speech service.
- Speaks replies through Android TextToSpeech when a Russian voice is installed.
- Runs a local rule-based command router; no AI API key or cloud account is required for the built-in commands.
- Stores user-defined command aliases and notes locally in app-private preferences.
- Supports short multi-step command phrases joined with "потом".
- Opens Android Wi-Fi, Bluetooth, VPN, location, accessibility, storage, sound, notification, display, and app-permission settings.
- Opens installed VPN clients in a priority order and can inspect Android's active network transport for a VPN tunnel. VPN activation still requires the user's action in the VPN app/system UI.
- Controls the flashlight after the user grants camera permission; adjusts media volume and sends play/pause/track commands to the active media session; reports battery level, device model, Android version, available app-data storage, and network transport.
- Copies or shares user-supplied text through Android's clipboard and share sheet; opens the dialer, prepares an SMS draft, and searches destinations in map apps. Calls and messages are never sent automatically.
- Can prefill a system alarm at a specified HH:MM time.
- Hands timers, alarms, calendar event creation, file selection, and web searches to Android's system apps.

## Safety and privacy model

- No hidden recording, keylogging, screen scraping, advertising SDKs, analytics SDKs, or remote command server is included.
- No arbitrary shell execution is provided.
- Notes and learned aliases remain in app-private local storage.
- Permissions are declared only for features that need them. Android still controls the permission prompts and system confirmation dialogs.
- Package visibility is requested to build the on-device app catalogue. This is intended for direct personal testing; Google Play distribution has additional package-visibility policy requirements.
- The app does not silently connect a VPN, bypass Android consent screens, or claim that a VPN is working based only on launching its app.

## Current limitations

- The command engine is a deterministic local command system, not a general-purpose LLM. It does not understand every free-form request or autonomously invent safe actions.
- Speech recognition depends on the Android speech service installed/configured on the phone; offline recognition availability depends on that service and its language pack.
- Android does not allow an ordinary app to control all other apps or change protected settings without specific user-granted access.
- A computer-control feature needs a separately installed desktop companion, explicit device pairing, and an authenticated local connection. That companion is not yet implemented.
- The current build does not include an always-listening hotword service or an accessibility automation service.

## Build

A GitHub Actions workflow is configured to build a debug APK on pushes to the main branch and upload it as an artifact named `JARVIS-debug-apk-and-build-log`. A successful build and APK artifact must be confirmed in Actions before this project can be described as install-ready.
