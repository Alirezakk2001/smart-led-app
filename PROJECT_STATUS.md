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

The app may cache it locally, but device-reported configuration is authoritative after synchronization.

Example:

```text
ESP32/device
  └── lineCount = 3
  └── line1.ledCount = 60
  └── line2.ledCount = 120
  └── line3.ledCount = 60
```

When another device/app connects, it can read the same configuration.

---

# 12. Configuration Changes

Changing hardware configuration should be explicit.

Example:

```text
Hardware Configuration

Lines: 4

Line 1: 60 LEDs
Line 2: 60 LEDs
Line 3: 60 LEDs
Line 4: 60 LEDs

[Apply]
```

The application must:

1. Validate against device capabilities.
2. Send configuration.
3. Wait for confirmation.
4. Refresh configuration/state.
5. Notify the user if restart/reinitialization is required.

Do not assume a configuration change succeeded merely because the BLE write succeeded.

---

# 13. BLE / Protocol

The exact BLE service/characteristic layout and command protocol are **not finalized yet**.

Do not invent protocol values during implementation.

The eventual architecture should separate:

```text
BLE Transport
Protocol Encoder/Decoder
Device Repository
Domain
UI
```

UI must never directly construct raw BLE packets.

---

# 14. Synchronization Model

Desired initial synchronization:

```text
Connect
 ↓
Capabilities
 ↓
Configuration
 ↓
Current State
 ↓
UI
```

Runtime command:

```text
User action
 ↓
Domain command
 ↓
Repository
 ↓
Protocol
 ↓
Transport
 ↓
Device
 ↓
Confirmation/state update
 ↓
Application state
 ↓
UI
```

The application must distinguish:

- confirmed state
- pending changes
- disconnected/unknown state

---

# 15. Offline / Disconnected Behavior

When disconnected:

- display disconnected status
- retain last known state where useful
- keep preview functional where possible
- allow local UI exploration
- distinguish local/pending state from hardware-confirmed state

On reconnect:

```text
Reconnect
 ↓
Re-read capabilities
 ↓
Re-read configuration
 ↓
Re-read state
```

---

# 16. Main Screens

## Dashboard

Primary screen.

Contains:

- device/connection status
- LED preview
- power
- brightness
- line selection
- current effect
- effect parameters
- color where applicable
- speed where applicable

This screen gets the strongest ambient visual treatment.

---

## Effects

Contains:

- effect list
- effect cards
- preview
- selected state
- supported parameters

Effects must be data-driven.

Adding a new hardware effect should not require rewriting the entire screen.

---

## Effect Editor

Dynamic controls based on effect capabilities.

Potential parameters:

- color
- speed
- brightness
- direction
- sensitivity
- effect-specific parameters

No universal hard-coded parameter set should be assumed.

---

## Devices

Contains:

- scan
- discovered devices
- connect
- disconnect
- device status
- device identity
- rename/forget where supported

The app should conceptually support multiple devices.

---

## Lines / Strips

Controls are generated dynamically.

Example:

```text
[All] [1] [2] [3] ... [N]
```

The number of controls is based on device configuration.

---

## Hardware Configuration

Contains:

- number of LED lines
- LED count per line
- supported LED type/configuration
- hardware-specific options exposed by capabilities

This is a settings/configuration area, not a primary daily control surface.

---

## Settings

Sections:

```text
Appearance
Animation
Ambient UI
Device
Hardware
Advanced
About
```

Settings screens should remain calm and mostly static.

---

# 17. Navigation

## Mobile

Preferred navigation:

```text
Home
Effects
Devices
Settings
```

Additional screens can be reached from these sections.

## Desktop

Preferred layout:

```text
Sidebar
 ├── Dashboard
 ├── Effects
 ├── Devices
 ├── Scheduler
 ├── Music
 └── Settings
```

Future sections may be hidden/disabled until implemented.

---

# 18. Visual Design

## Design Direction

**Ambient Dark UI**

The UI should be:

- dark
- minimal
- modern
- ambient
- reactive
- restrained

Avoid:

- overly gamified visuals
- excessive neon
- cyberpunk clutter
- constant RGB cycling
- unnecessary decorative animation

---

# 19. Base Design Tokens

Suggested dark palette:

```text
Background       #0B0D10
Surface          #12151A
Surface Elevated #181C22

Text Primary     #F5F7FA
Text Secondary   #A8AFBA
Text Disabled    #626974
```

These must eventually become centralized design tokens.

Do not scatter raw color literals throughout UI code.

---

# 20. Dynamic Accent

The application should derive accent colors from the actual LED state.

Example:

```text
LED red
 → red accent

LED blue
 → blue accent

LED purple
 → purple accent
```

The system should derive a palette:

```text
Accent
Accent Soft
Accent Strong
Glow
Ambient
```

The raw LED color must not blindly be used for text if it causes poor contrast.

---

# 21. Ambient Background

Dashboard/Effects may contain a very subtle ambient background.

Suggested implementation concepts:

- radial gradients
- blur
- low opacity
- slow transitions
- multiple color sources

Starting token targets:

```text
Ambient background ≈ 3%
Glow ≈ 6%
```

These are tunable values.

The effect should be felt, not obviously seen as a gradient layer.

---

# 22. Multiple Line Ambient Behavior

If lines have different colors:

```text
Line 1 → Red
Line 2 → Blue
Line 3 → Purple
Line 4 → Green
```

the app should aggregate these into an ambient palette.

Transitions should be slow and smooth.

Do not rapidly cycle the whole UI through line colors.

---

# 23. Effect-Aware Ambient Behavior

Examples:

```text
Static
→ stable glow

Pulse
→ subtle slow intensity modulation

Rainbow
→ slow color drift

Fire
→ warm slow movement

Ocean
→ slow cyan/blue movement
```

Ambient animation must be calmer than the hardware effect.

---

# 24. Calm Utility Screens

These should not receive full ambient animation:

- Settings
- Hardware Configuration
- Devices
- About
- Advanced configuration

They should prioritize:

- readability
- stable surfaces
- hierarchy
- accessibility

---

# 25. LED Preview

Preview should represent the actual configured hardware.

Dynamic dimensions:

```text
number of lines
LED count per line
```

Each LED can be represented as:

```text
core
+
subtle glow
```

Preview should support:

- power
- brightness
- colors
- effects
- line selection

It should be usable without a live device where practical.

---

# 26. Animation System

Three categories:

### Micro UI

```text
150–250ms
```

For normal UI transitions.

### Ambient

```text
2–8s
```

For background/glow transitions.

### Effect Preview

Driven by selected effect, but intentionally calmer than hardware.

Prefer smooth easing.

Avoid excessive linear animation.

Support:

```text
Reduced Motion
```

---

# 27. Themes

Support:

- System
- Light
- Dark

Dark is the primary design.

Suggested light tokens:

```text
Background #F6F7F9
Surface    #FFFFFF
Text       #15181D
```

Ambient effects should be significantly weaker in Light mode.

---

# 28. Accessibility

Must consider:

- contrast
- readable typography
- touch target size
- keyboard interaction
- focus states
- Reduced Motion
- color-independent state communication
- clear connection states

---

# 29. Responsive UI

Mobile:

```text
single/compact column
touch-first
mobile navigation
```

Tablet:

```text
multi-column where appropriate
```

Desktop:

```text
sidebar
multi-column
resizable window
mouse + keyboard
```

Use shared CMP components and adaptive layouts.

---

# 30. Persistence

## Device-side

Potentially:

- LED line count
- LED counts
- hardware configuration
- device settings

## App-side

Potentially:

- remembered devices
- theme
- ambient preference
- reduced motion
- last-known state
- UI preferences

Device-confirmed state must not be confused with local cache.

---

# 31. Error Handling

Expected errors include:

- scan failure
- connection failure
- disconnect
- timeout
- command rejection
- invalid configuration
- unsupported feature
- protocol mismatch
- firmware incompatibility

User-facing errors should be actionable.

Example:

```text
Unable to connect

[Retry]
```

Avoid showing raw BLE/protocol exceptions to normal users.

---

# 32. Versioning

The design must support:

```text
App Version
Firmware Version
Protocol Version
Capability Version
```

Compatibility checks should occur before unsupported operations.

The app should not assume all devices use the latest firmware/protocol.

---
# 33. Architecture Status

### Implemented structure

The package/module structure is implemented as follows:

```
shared/src/commonMain/kotlin/com/technest/smartled/
├── App.kt                          ← Compose UI scaffold + NavigationBar
├── Platform.kt                     ← expect declarations
├── core/
│   ├── model/                      ← Domain models (hardware-agnostic)
│   │   ├── Brightness.kt
│   │   ├── Color.kt
│   │   ├── Device.kt
│   │   ├── DeviceCapabilities.kt
│   │   ├── DeviceConfiguration.kt
│   │   ├── DeviceState.kt
│   │   ├── EffectId.kt
│   │   ├── EffectParameter.kt
│   │   └── LedLineState.kt
│   └── domain/
│       └── Navigation.kt           ← Screen sealed class
├── data/
│   ├── repository/
│   │   └── DeviceRepository.kt     ← Repository interface
│   └── transport/
│       └── Transport.kt            ← Transport abstraction interface
├── feature/
│   ├── dashboard/DashboardScreen.kt
│   ├── devices/DevicesScreen.kt
│   ├── effects/EffectsScreen.kt
│   ├── settings/SettingsScreen.kt
│   └── setup/SetupScreen.kt
└── ui/theme/
    ├── Color.kt                    ← Centralized color tokens
    ├── Theme.kt                    ← M3 Dark/Light color schemes
    └── Type.kt                     ← Typography styles
```

### Confirmed direction (unchanged)

```text
Compose UI
 ↓
ViewModel / Presentation
 ↓
Domain / Use Cases
 ↓
Repository Interfaces
 ↓
Protocol / Data
 ↓
Transport
 ↓
Hardware
```

The next architectural layer to implement is the presentation/ViewModel layer.

---

# 34. Planned Project Structure

Initial target:

```text
composeApp/

core/
  common/
  model/
  domain/

data/
  ble/
  protocol/
  repository/
  local/

feature/
  dashboard/
  devices/
  effects/
  strips/
  scheduler/
  music/
  settings/
  setup/

platform/
  android/
  ios/
  desktop/
```

This is a direction, not a requirement to create every directory before it is needed.

---

# 35. Testing Strategy

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

# 36. Git / Commit Strategy

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

# 37. Current Implementation State

Status as of the initial commit:

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
- Exact package structure: **implemented** (see section 33)
- Exact BLE services/characteristics: **pending hardware/application coordination**


# 38. Next Major Phase

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

# 39. Definition of Done for Initial App Foundation

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

# 40. Project Rules Summary

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
