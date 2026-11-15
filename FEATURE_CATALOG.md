# JARVIS capability catalogue and implementation plan

This document records capability ideas gathered from public assistant projects. It describes functions and design patterns only. JARVIS source code is implemented independently; no third-party assistant source files are copied.

## Capability families to consider

### Local assistant core
- Local model selection and model-size/device capability checks.
- Local intent routing for fast, predictable commands without model inference.
- Optional on-device language model for flexible language understanding when hardware permits.
- Conversation context, editable long-term memory, notes, and document search.
- Tool selection, multi-step planning, execution feedback, retries, and final result verification.
- Skills/command packs with explicit permissions and versioned manifests.

### Voice
- Push-to-talk as the reliable baseline.
- Local speech recognition and local text-to-speech when a suitable language model is installed.
- Optional wake word only after battery, privacy, and background-execution testing.
- Interruptible spoken responses and configurable voice/language/speed.

### Android device control
- Installed-app catalogue with real launcher icons.
- App launch and app-category routing.
- System settings handoff for Wi-Fi, Bluetooth, location, sound, notifications, display, storage, accessibility, and VPN.
- Battery/network state, media volume, flashlight, timers, alarms, calendar handoff, and file picker.
- Optional accessibility automation only after the user explicitly enables the service in Android Settings. Screen text must remain on-device and only be inspected for a user-requested task.
- Notification access only as a separate opt-in capability with a clear explanation and per-feature controls.

### Windows/desktop companion
- System status: CPU, memory, storage, network, battery where applicable.
- Open approved applications, switch windows, and control media/volume.
- Search and open user-selected files; summarize documents only after explicit selection.
- Browser launch and visible browser automation.
- Clipboard read/write only on explicit user command.
- Optional screen/OCR assistance with visible indication and explicit capture approval.
- Task plans with step progress, error recovery, and a final verification report.
- Scheduled routines and reminders with clear on/off controls.

### Cross-device bridge
- Pair devices through a user-confirmed one-time code or QR.
- Authenticated encrypted local connection; never expose a desktop control port to the public internet by default.
- Device presence/status and explicit task handoff.
- Per-device permission list, revocation, and visible connection state.
- Allowlisted desktop actions; no unrestricted remote shell or arbitrary downloaded scripts.

## Implementation status in this repository

| Capability | Status |
|---|---|
| Native Android shell and dark interface | Source added; build not yet verified |
| App catalogue and icons | Source added |
| App launch and basic name matching | Source added |
| Voice intent handoff and Android TTS | Source added; offline behavior depends on installed speech/voice services |
| Local notes and learned command aliases | Source added |
| Bounded multi-step command phrases and loop guard | Source added |
| Battery, volume, flashlight, network/VPN transport checks | Source added |
| Android settings, timer, alarm, calendar and file-picker handoff | Source added |
| Local language model | Not implemented |
| Android accessibility automation | Not implemented |
| Always-listening wake word | Not implemented |
| Windows companion and secure pairing | Not implemented |
| Cross-device memory/task sync | Not implemented |

## Development order

1. Get a reproducible successful Android debug build and inspect its logs.
2. Add unit-tested command routing, task-step state, cancellation, and result verification.
3. Add a model-free intent/slot parser and editable skill definitions.
4. Evaluate an optional local model only after measuring memory, storage, startup time, and battery use on the target phone. A local LLM is optional, not a hidden dependency.
5. Add Android accessibility actions as a separately enabled, visibly active, opt-in service.
6. Build the Windows companion with a narrow allowlist, secure pairing, and revocation.
7. Add cross-device handoff and synchronization only after the security model is tested.

## Public capability references

These projects are used only to understand publicly described feature categories and product patterns:
- O.A.S.I.S. — local desktop assistant, voice, memory, everyday PC control, documents and workflows: https://github.com/OASISLocal/O.A.S.I.S.
- Open.Jarvis — local command routing, voice/text, TTS, memory and desktop integrations: https://github.com/dmrr35/Open.Jarvis
- Thoth — local-first tools, memory, workflows, approvals and multi-channel architecture: https://github.com/tokwalabs/Thoth
- OpenDesk — paired desktop control, UI access, OCR and task recording: https://github.com/vitalops/opendesk

The presence of a feature in a reference project does not mean it is safe, available, or appropriate to copy wholesale. Each capability must be implemented independently and reviewed against Android/Windows permissions, user consent, reliability, and device performance.
