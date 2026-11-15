# JARVIS Android

A native Kotlin Android assistant prototype. It is designed to work without AI API keys.

## Current implementation
- Lists launchable apps installed on the device, including their names and icons.
- Launches an app by matching its displayed name.
- Accepts typed commands and Android speech-recognition results.
- Speaks responses with Android TextToSpeech when a Russian voice is installed.
- Opens Android Wi-Fi, Bluetooth, VPN and general settings screens.
- Finds likely VPN clients by installed app label/package name and opens a candidate.
- Handles basic date/time and web-search commands.
- Uses a small local rule-based command router; it does not call a cloud model.

## Important limitations
- This is a command-based prototype, not a general-purpose autonomous AI model. It cannot reliably understand arbitrary requests or learn new capabilities by itself.
- Android requires the user to approve VPN connections in the VPN app/system dialog. This app cannot silently connect, inspect every VPN's live state, or guarantee that a client works.
- App visibility is limited by Android package-visibility rules and device policy. This build requests broad package visibility for personal sideload testing; distribution through Google Play would require policy review.
- Reading or controlling another app's screen requires a separately enabled Android Accessibility Service and careful user-granted permissions. That service is not implemented in this first build.
- Actions that change settings, send data, delete content, make purchases, or affect accounts must not be performed speculatively. A future task planner should ask for confirmation before consequential actions.

## Build
Pushes to main run the Android workflow. The workflow builds a debug APK and uploads it as the JARVIS-debug-apk artifact. A debug APK is for personal testing, not a Play Store release.
