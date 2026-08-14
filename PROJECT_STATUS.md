# PROJECT_STATUS.md

# LED Controller Application — Project Status

## 1. Project Overview

This project is the cross-platform application for the LED controller hardware project.

The application will be built with:

- Kotlin Multiplatform (KMP)
- Compose Multiplatform (CMP)

Target platforms:

- Android
- iOS
- Windows
- Linux
- macOS

The current hardware implementation is based on an ESP32 and WS2812B-class addressable RGB LEDs, but the application is intentionally designed to be **hardware-independent** so a different controller can be supported later.

---

# 2. Product Vision

The application is not intended to be a simple BLE remote.

It is a polished cross-platform LED control application with:

- device management
- configurable LED hardware
- dynamic number of LED lines
- per-line LED count
- effects
- brightness/color controls
- live LED preview
- dynamic accent colors
- ambient UI that reflects LED state
- future scheduling
- future music-reactive lighting

The visual identity is based on:

> **Dark + Minimal + Ambient + Reactive**

The LED state should influence the UI, while utility/configuration screens remain calm and readable.

---

# 3. Platforms

| Platform | Target |
|---|---|
| Android | Yes |
| iOS | Yes |
| Windows | Yes |
| Linux | Yes |
| macOS | Yes |

The UI should be shared through Compose Multiplatform.

Platform-specific implementations should be isolated behind common interfaces.

---

# 4. Current Product Scope

## V1 — Planned

### Device

- BLE/device discovery
- Connect/disconnect
- Device status
- Device identity
- Device configuration discovery
- Device capabilities discovery
- State synchronization

### Hardware Configuration

- Dynamic number of LED lines
- LED count per line
- Supported LED/hardware configuration
- Device-side persistence where supported

### Runtime Control

- Power
- Brightness
- Color where supported
- Effect
- Effect parameters
- Per-line control
- All-lines control

### Visual

- Dashboard
- Effects browser
- Effect editor
- LED preview
- Dynamic accent
- Ambient background
- Smooth ambient transitions

### Application

- Settings
- Appearance settings
- Animation settings
- Device settings
- Hardware settings
- Local persistence

---

# 5. Future Features

Only the following product features are reserved for the future:

## Scheduler

Planned capabilities:

- schedule creation
- time
- days
- power actions
- effect changes
- brightness
- enable/disable

## Music Reactive

Planned capabilities:

- audio input
- spectrum visualization
- beat detection/reactivity
- sensitivity
- reactive effects

### Explicitly Removed From Roadmap

Do not implement or plan these unless the project owner explicitly re-adds them:

- WiFi
- OTA
- Presets
- MQTT
- Web UI
- IR Remote

---

# 6. Hardware Independence

The application must not be tied to ESP32.

Current:

```text
ESP32
  ↓
BLE
  ↓
Application
```

Desired architecture:

```text
Device
  ↓
Transport
  ↓
Protocol
  ↓
Repository
  ↓
Domain
  ↓
UI
```

A future controller could be:

```text
ESP32
Other MCU
Other SBC
Other controller
```

without requiring a redesign of the application domain/UI.

---

# 7. Device Model

The application should conceptually support:

```text
Device
├── identity
├── connectionState
├── capabilities
├── configuration
└── currentState
```

### DeviceCapabilities

Potential fields:

```text
maxLines
maxLedsPerLine
supportedLedTypes
supportedEffects
supportsScheduler
supportsMusicReactive
protocolVersion
firmwareVersion
```

The exact protocol fields are pending hardware/application protocol design.

---

# 8. Dynamic LED Lines

The number of LED lines is configurable.

There is no fixed assumption of 1, 2, or 3 lines.

Example:

```text
1 line
2 lines
3 lines
4 lines
...
N lines
```

The UI must generate line controls dynamically from device configuration.

Preferred domain representation:

```text
List<LedLineState>
```

rather than fixed properties such as:

```text
line1
line2
line3
```

---

# 9. LED Count Configuration

Every line may have a different number of LEDs.

Example:

```text
Line 1 → 60
Line 2 → 120
Line 3 → 30
```

LED count belongs to Hardware Configuration.

It should not be exposed as a frequently used runtime control.

Location:

```text
Settings
 → Hardware
   → LED Configuration
```

---

# 10. Initial Setup

Preferred default:

```text
1 LED line
```

When a device is connected for the first time:

```text
Connect
 ↓
Read capabilities
 ↓
Read configuration
 ↓
Determine whether setup is required
```

If unconfigured:

```text
Welcome
 ↓
Number of lines
 ↓
LED count for each line
 ↓
Other required hardware parameters
 ↓
Validate
 ↓
Apply
 ↓
Persist
 ↓
Synchronize
```

If already configured, skip setup and enter the normal application.

---

# 11. Device Configuration Ownership

Hardware configuration should preferably be stored on the device.

The app may cache it locally, but device-persisted configuration is authoritative.

---

# 12. State Synchronization

When connected:

```text
Connect
 ↓
Read capabilities
 ↓
Read configuration
 ↓
Read current state
 ↓
Expose confirmed state to UI
```

When a command is sent:

```text
User Action
 ↓
Command
 ↓
Device
 ↓
Confirmation / updated state
 ↓
App State
 ↓
UI
```

The app does not treat sent commands as confirmed state.

---

# 13. Offline / Disconnected Behavior

When disconnected:

- the UI shows a clear disconnected state
- the last known state is retained where useful
- the LED preview remains usable
- pending changes are distinguished from confirmed device state
- reconnection triggers synchronization

---

# 14. Error Handling

Errors should be represented as user-readable states, not raw exceptions.

Examples:

```text
Unable to connect
[Retry]
```

```text
Configuration rejected
The device supports up to 4 lines.
```

No raw protocol/BLE errors should be exposed to normal users.

---

# 15. Versioning and Compatibility

Planned:

- app version
- firmware version
- protocol version
- device capability version

Protocol changes must be versioned.

Compatibility checks should happen before unsupported operations.

---

# 16. Testing Strategy

Each meaningful task must be verified before moving to the next task.

Workflow:

```text
Implement
 ↓
Test / verify
 ↓
Fix
 ↓
Test again
 ↓
Update PROJECT_STATUS.md
 ↓
Commit
```

Important test areas:

- domain logic
- state transformations
- protocol encoder/decoder
- configuration validation
- capability handling
- color palette generation
- ambient color calculations
- ViewModel state
- repository behavior
- UI behavior
- platform-specific implementations

---

# 17. Git / Commit Strategy

Keep commits logical and focused.

Preferred:

```text
Feature/change
 ↓
Verification
 ↓
Commit
```

Do not commit known-broken code.

Do not mix unrelated changes.

---

# 18. Current Implementation State

Status as of the current commit:

- Product concept: defined
- Main screens: defined
- UI visual direction: defined
- Dynamic LED line concept: defined
- Dynamic LED count concept: defined
- Hardware-independent architecture: defined
- Device/capability model: **implemented** (Device, DeviceCapabilities, DeviceConfiguration, DeviceState, EffectId, EffectParameter, LedLineState, Brightness, RgbColor)
- Ambient UI concept: defined
- LED preview concept: defined
- Future Scheduler: reserved
- Future Music Reactive: reserved
- BLE/application protocol: **not finalized** (pending hardware/application coordination)
- App implementation: **App Foundation milestone complete** — KMP project scaffolded, domain models created, device abstraction + repository/transport interfaces defined, theme/design tokens centralized, Compose UI scaffold with navigation and placeholder screens, testing infrastructure established
- Device Management: **implemented** — MockTransport, DeviceRepositoryImpl, DevicesViewModel, DevicesScreen with scan/connect/disconnect UI, 40 passing tests
- Exact package structure: **implemented** (see section 33)
- Exact BLE services/characteristics: **pending hardware/application coordination**
- Dashboard: **implemented** — DashboardViewModel with power/brightness/line/effect/color/speed controls, line selector (All + individual), LED preview, color picker, effect dropdown, 15 new tests
- DeviceRepositoryImpl: **updated** — now tracks device state (power, brightness, lines) and emits state changes through the connected device flow
- Effects: **implemented** — EffectsViewModel with effect browsing, descriptions, per-effect parameter editor (speed sliders), apply-to-line and apply-to-all actions, 16 new tests
- **Settings: implemented** — SettingsViewModel with appearance (theme: System/Dark/Light), device info (read-only), hardware configuration (line count ±, LEDs per line ±, apply button), and about section. SettingsScreen is calm (no ambient motion, per AGENTS.md). 13 new tests. ThemeMode enum added. LedTheme accepts ThemeMode. App.kt wires up theme state and SettingsViewModel.
- **Setup: implemented** — SetupViewModel with line count configuration, per-line LED count, capability-aware clamping (maxLines/maxLedsPerLine), apply configuration, and completion flow. SetupScreen with device info card, line/LED selectors, error handling, and completion confirmation. 14 new tests. Wired into App.kt navigation.

# 19. Next Major Phase

Before implementation, define the **App ↔ Hardware Contract**.

This phase must specify:

### Device discovery

- device identity
- device type
- capabilities

### Configuration

- line count
- LED count per line
- LED type
- hardware limits

### Runtime state

- power
- brightness
- color
- effect
- effect parameters
- per-line state

### Commands

- configuration commands
- runtime commands
- state queries
- synchronization

### Notifications

- state changes
- configuration changes
- errors

### Errors

- validation
- unsupported operation
- protocol mismatch
- device errors

### Versioning

- protocol version
- firmware compatibility
- capability negotiation

### BLE

- services
- characteristics
- read/write/notify behavior
- packet framing
- payload limits
- acknowledgements

No implementation should invent these details before they are agreed with the hardware side.

# 20. Definition of Done for Initial App Foundation

The initial foundation is considered complete when:

- KMP project builds
- CMP UI runs on intended targets as far as the local environment allows
- common/domain layers exist
- device abstraction exists
- dynamic line model exists
- device configuration model exists
- capability model exists
- repository boundaries exist
- platform-specific transport boundary exists
- theme/design tokens are centralized
- initial navigation exists
- testing/verification is established
- project documentation is updated
- successful work is committed

---

# 21. Project Rules Summary

The application must remain:

- cross-platform
- hardware-independent
- state-driven
- dynamically configurable
- visually reactive where appropriate
- calm in utility screens
- extensible for future effects
- extensible for Scheduler
- extensible for Music Reactive
- testable
- maintainable

The most important constraints are:

```text
No fixed number of lines.
No fixed LED count.
No ESP32 dependency in domain/UI.
No raw BLE logic in UI.
No unconfirmed hardware state presented as confirmed.
No distracting ambient animation.
No invented protocol details.
Test after meaningful tasks.
Commit only after successful verification.
```
