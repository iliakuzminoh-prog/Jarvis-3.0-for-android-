# Security model

JARVIS is intended as a user-controlled local Android utility.

## Included behavior
- Runs built-in Kotlin command handlers inside the app process.
- Uses Android intents to hand selected tasks to system or user-installed apps.
- Stores command aliases and notes in app-private SharedPreferences.
- Reads installed launchable app metadata using Android package-visibility queries for launcher activities.
- Reads battery and active-network information through Android APIs.
- Requests camera permission only when the user asks to toggle the flashlight.

## Explicitly not included
- No hidden microphone recording or always-on hotword listener.
- No keylogging, hidden screen capture, or background surveillance.
- No ad SDK, analytics SDK, tracking beacon, or remote command endpoint.
- No arbitrary shell command execution, downloaded scripts, or self-updating executable payloads.
- No automatic VPN installation or silent VPN activation.
- No transfer of notes or learned commands to a server.
- Internal floating-overlay actions require an app-private rotating one-time token; unauthenticated external intents are ignored.
- The embedded browser accepts HTTPS navigation only, blocks mixed content, disables file/content access, enables Android Safe Browsing, and exposes no JavaScript interface.
- App data backup is disabled in the manifest.

## External handoffs
When the user asks for web search, JARVIS opens the Android browser with the query. Timer, alarm, calendar, file, and speech recognition tasks are handed to the relevant Android/system app. Those apps operate under their own privacy policies and permissions.

## Permissions
- CAMERA: requested at runtime only for the flashlight feature.
- ACCESS_NETWORK_STATE: reads whether Android reports a VPN transport or another network transport.
- App discovery is limited by Android package-visibility rules to launchable activities; the app does not request QUERY_ALL_PACKAGES.
- Local notes in SharedPreferences are app-private but are not separately encrypted. Do not store passwords, authentication tokens, or payment details in JARVIS memory.

Android controls runtime permissions, VPN consent, and protected system settings. JARVIS cannot grant itself permissions.

## Build trust
The project uses Android Gradle Plugin, Kotlin, AndroidX AppCompat/Core, and official GitHub Actions build steps. A debug APK is not a Play Store release. Review the build log and artifact checksum; scan the produced APK with reputable security tools before installing. No software can be honestly guaranteed malware-free solely from its source repository.
