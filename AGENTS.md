# AGENTS.md

# LED Controller Application — Agent Instructions

## 1. Project Purpose

This repository contains the cross-platform application for the LED controller hardware project.

The application is a **Kotlin Multiplatform (KMP)** project using **Compose Multiplatform (CMP)** for the UI.

Target platforms:

- Android
- iOS
- Windows
- Linux
- macOS

The application communicates with LED-controller hardware primarily through an abstract device/BLE layer. The app must **not be tightly coupled to ESP32**. ESP32 is the current hardware implementation, but another controller/platform must be usable later without redesigning the application.

The app controls one or more independently configurable LED lines/strips. The number of lines and the number of LEDs on each line are **runtime/device configuration**, not hard-coded application assumptions.

---

## 2. Core Product Principles

### 2.1 Hardware-independent application

Never build domain or UI logic around assumptions such as:

- exactly 3 LED lines
- exactly one device
- exactly 60 LEDs per line
- ESP32 being the permanent controller
- one fixed BLE implementation
- a fixed set of effects

The application must work from a device's reported:

- capabilities
- configuration
- current state
- protocol/version information

Use abstractions such as:

- `Device`
- `DeviceCapabilities`
- `DeviceConfiguration`
- `LedLine`
- `LedLineState`
- `Effect`
- `EffectParameter`
- `DeviceState`

rather than ESP32-specific domain objects.

### 2.2 Single source of truth

The actual device state is authoritative.

The application may maintain cached/local state for UX, previews, and offline operation, but it must distinguish between:

- confirmed device state
- pending/local changes
- disconnected state

When a device is connected, the app should synchronize with the device and build its UI from the confirmed configuration/state.

Do not silently assume the app's previous state is still the hardware state.

### 2.3 UI is reactive to the LED state

The application should feel like a living LED controller, not a generic CRUD application.

The visual system is driven by:

- current LED colors
- active effects
- line states
- power state
- brightness
- device connection state

This drives:

- dynamic accent
- subtle ambient background
- LED preview
- selected-state visuals
- appropriate animation

### 2.4 Calm settings UI

Reactive/ambient visuals are strongest on control-oriented screens such as Dashboard and Effects.

Settings, Hardware Configuration, Device Management, About, and similar utility screens must remain visually calm.

Do not let large or fast effects run behind configuration forms.

Ambient behavior may be disabled or reduced on utility screens.

---

## 3. Technology Requirements

Primary stack:

- Kotlin Multiplatform
- Compose Multiplatform
- Kotlin Coroutines
- Kotlin Flow / StateFlow where appropriate

Architecture should follow clean separation between:

- presentation
- domain
- data
- platform-specific implementations

Use `expect/actual` only where platform-specific behavior genuinely requires it.

Do not leak platform-specific APIs into common business/domain code.

---

## 4. Platform Targets

The project must be structured for:

```text
Android
iOS
Windows
Linux
macOS
```

Desktop UI must support:

- mouse
- keyboard
- resizable windows
- different window sizes
- appropriate desktop navigation

Mobile UI must support:

- touch interaction
- compact layouts
- appropriate navigation

Do not create five unrelated UIs. Use a shared Compose UI with responsive/adaptive layouts and platform-specific implementations only where required.

---

## 5. Recommended Architecture

Preferred high-level structure:

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

Exact package names may evolve, but the architectural boundaries must remain.

Recommended dependency direction:

```text
UI
 ↓
ViewModel / Presentation
 ↓
Use Cases / Domain
 ↓
Repositories / Interfaces
 ↓
Data / Protocol / Platform
 ↓
Hardware
```

Do not reverse these dependencies.

---

## 6. Device Abstraction

The app must expose a hardware-independent device API.

Conceptually:

```text
Device
├── identity
├── connectionState
├── capabilities
├── configuration
└── currentState
```

Capabilities should be discoverable from the device and may include:

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

Do not hard-code capability limits in UI unless they are explicit application safety limits.

---

## 7. Dynamic LED Lines

The number of LED lines is configurable.

Examples:

```text
1 line
2 lines
3 lines
...
N lines
```

The UI must render lines dynamically.

Never use fixed fields such as:

```text
strip1
strip2
strip3
```

as the domain model.

Use collections:

```text
lines: List<LedLineState>
```

The device is responsible for declaring supported limits.

---

## 8. LED Count Per Line

Each line has its own configurable LED count.

Example:

```text
Line 1 → 60 LEDs
Line 2 → 120 LEDs
Line 3 → 30 LEDs
```

LED count is a hardware configuration setting.

It should be accessible from:

```text
Settings
  → Hardware
    → LED Configuration
```

It should not be presented as a daily runtime control beside color/effect/brightness.

The configuration should be persisted by the device when the hardware supports persistent configuration.

---

## 9. Initial Setup

The first connection/setup experience should be simple.

Preferred behavior:

1. Connect to the device.
2. Read device capabilities/configuration.
3. If the device is unconfigured, use a safe/default configuration.
4. Offer a setup flow to configure:
   - number of lines
   - LED count per line
   - supported hardware parameters
5. Validate against device capabilities.
6. Apply configuration.
7. Confirm successful persistence.
8. Refresh device state/configuration.

A default of **one line** is preferred for initial simplicity.

Do not assume that setup is always required. A preconfigured device must open directly into the normal application.

---

## 10. BLE / Transport Abstraction

BLE is a transport mechanism, not the domain model.

Use a layered approach:

```text
UI
 ↓
Repository
 ↓
Device Service
 ↓
Transport
 ↓
Protocol Encoder/Decoder
 ↓
BLE implementation
```

The protocol must be isolated from Compose UI.

UI code must never manually construct raw BLE packets.

The protocol layer should own:

- encoding commands
- decoding responses
- state messages
- errors
- versioning
- validation

The transport layer should own:

- scan
- connect
- disconnect
- read/write/notify
- connection state
- platform-specific BLE implementation

---

## 11. State Synchronization

The app must support:

- initial state synchronization
- configuration synchronization
- runtime state synchronization
- reconnect synchronization
- device-originated changes where supported
- confirmation of commands

Preferred flow:

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

After a command:

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

Do not treat a command as confirmed merely because it was sent successfully.

---

## 12. Offline / Disconnected Behavior

When disconnected:

- show clear disconnected state
- retain last known state where useful
- keep LED preview usable
- allow local configuration of UI where appropriate
- distinguish pending changes from confirmed device state

Do not falsely display a disconnected device as synchronized.

Reconnect should trigger synchronization.

---

## 13. Screens

### Dashboard

Primary control screen.

Should provide:

- connection status
- device identity
- power
- brightness
- selected line(s)
- effect
- effect parameters
- color where applicable
- speed where applicable
- LED preview
- quick controls

Dashboard should feel alive and use the ambient visual system.

### Effects

Show available effects using cards/list items.

Each effect should expose only parameters supported by that effect.

The architecture should allow new effects without requiring a complete UI rewrite.

### Effect Editor

Dynamic parameter UI.

Examples:

- speed
- direction
- color
- brightness
- sensitivity
- effect-specific parameters

Do not hard-code a single parameter set for all effects.

### Devices

Provide:

- scanning
- discovered devices
- connect
- disconnect
- rename where supported
- forget/remove
- connection status

Support multiple devices conceptually even if V1 focuses on one active device.

### Strips / Lines

Provide dynamic line selection/control.

Possible selection:

```text
All
1
2
3
...
N
```

The UI must adapt automatically to the configured number of lines.

### Hardware Configuration

Provide:

- number of lines
- LED count per line
- supported LED type/configuration
- other hardware-specific configuration exposed by capabilities

Changes should be validated and clearly confirmed.

### Scheduler — Future

Reserve architecture/navigation for:

- schedules
- days
- time
- actions
- effect selection
- brightness
- power state

Do not implement unless explicitly requested.

### Music Reactive — Future

Reserve architecture/navigation for:

- audio input
- beat/spectrum modes
- sensitivity
- reactive effects

Do not implement unless explicitly requested.

### Settings

Possible sections:

- Appearance
- Animation
- Ambient UI
- Device
- Hardware
- Advanced
- About

Settings should remain calm and low-motion.

---

## 14. Visual Design System

Visual direction:

**Ambient Dark UI**

Characteristics:

- dark
- minimal
- modern
- subtle futuristic feel
- ambient
- not cyberpunk
- not overly gamified
- LED state is the visual focus

### Base colors

Suggested tokens:

```text
Background       #0B0D10
Surface          #12151A
Surface Elevated #181C22

Text Primary     #F5F7FA
Text Secondary   #A8AFBA
Text Disabled    #626974
```

These are design tokens, not values to scatter throughout code.

### Dynamic Accent

Do not use a permanently fixed primary accent.

Derive an accent palette from current LED state:

```text
LED state
 ↓
Color engine
 ↓
Accent
Accent Soft
Accent Strong
Glow
Ambient
```

The derived accent must preserve readability and accessibility.

Do not blindly use raw LED RGB as text/button color.

### Ambient Background

Dashboard/Effect screens may use:

- radial gradients
- blur
- very low opacity
- slow transitions
- multiple ambient sources for multiple lines

Ambient must remain subtle.

Suggested starting values:

```text
ambient background alpha ≈ 3%
ambient glow alpha ≈ 6%
```

These are tunable design tokens, not hard-coded magic numbers.

### Multiple line colors

If lines have different colors/effects:

```text
Line 1 → red
Line 2 → blue
Line 3 → purple
Line 4 → green
```

aggregate them into an ambient palette.

Transition slowly and imperceptibly.

Do not make the whole UI rapidly cycle through RGB.

---

## 15. Effect-Aware Ambient UI

Ambient behavior may reflect the active effect:

- Static → stable glow
- Pulse → very subtle slow intensity modulation
- Rainbow → slow color drift
- Fire → slow warm movement
- Ocean → slow cyan/blue movement

The UI animation must be substantially calmer than the actual LED animation.

The app must never become visually distracting because an LED effect is fast.

---

## 16. LED Preview

The application should include a visual LED preview.

Preview must support dynamic:

- line count
- LED count
- line colors
- effects
- brightness
- power state

Conceptually:

```text
Line 1: ● ● ● ● ● ● ●
Line 2: ● ● ● ● ● ● ●
Line 3: ● ● ● ● ● ● ●
```

LEDs should visually resemble illuminated pixels using a core + subtle glow.

Where practical, the preview can continue working without a live connection using the last known/local state.

---

## 17. Animation Rules

Use three broad animation classes:

### Micro UI

Approx:

```text
150–250ms
```

For:

- navigation
- buttons
- toggles
- sliders
- selection

### Ambient

Approx:

```text
2–8s
```

For:

- background color transitions
- glow movement
- ambient intensity

### Effect Preview

Driven by the selected effect, but intentionally calmer than hardware animation.

Prefer smooth easing and organic transitions.

Avoid unnecessary linear animations.

Support a Reduced Motion setting.

---

## 18. Utility Screens

Settings, Hardware Configuration, Device Management, About, and similar screens should:

- minimize ambient motion
- avoid animated gradients
- prioritize readability
- use stable surfaces
- use clear hierarchy

Dynamic accent may remain available, but should be restrained.

---

## 19. Themes

Support:

- System
- Light
- Dark

Dark is the primary visual experience.

Light mode should retain dynamic accent but use much weaker ambient effects.

Suggested light tokens:

```text
Background #F6F7F9
Surface    #FFFFFF
Text       #15181D
```

---

## 20. Accessibility

Consider:

- sufficient contrast
- readable typography
- touch target sizes
- keyboard navigation on desktop
- focus states
- Reduced Motion
- not relying on color alone to communicate state
- clear connected/disconnected indicators

---

## 21. Responsive / Adaptive Layout

Mobile:

- compact layout
- bottom navigation or equivalent mobile navigation
- touch-first controls

Tablet:

- multi-column where useful

Desktop:

- sidebar navigation
- multi-column content
- resizable layout
- mouse/keyboard friendly interactions

Do not duplicate entire screens just to support different window sizes.

---

## 22. Data Persistence

Separate:

### Device-persisted data

Examples:

- line count
- LED count
- hardware configuration
- device settings

### App-local data

Examples:

- remembered devices
- UI preferences
- theme
- ambient UI preference
- reduced motion
- last selected screen
- cached last-known state

Never confuse app-local cache with confirmed hardware state.

---

## 23. Error Handling

Errors should be represented explicitly.

Examples:

- scanning failed
- connection failed
- disconnected
- command rejected
- invalid configuration
- unsupported feature
- protocol mismatch
- firmware incompatibility
- timeout

Prefer user-readable states/actions over raw exceptions.

Examples:

```text
Unable to connect
[Retry]
```

or:

```text
Configuration rejected
The device supports up to 4 lines.
```

Do not expose raw protocol/BLE errors directly to normal users unless useful in diagnostics.

---

## 24. Versioning and Compatibility

Plan for:

- app version
- firmware version
- protocol version
- device capability version

Protocol changes must be versioned.

Do not assume the latest app talks only to the latest firmware.

Compatibility checks should happen before unsupported operations.

---

## 25. Testing

Every meaningful implementation task must be followed by verification.

Minimum process:

```text
Implement task
 ↓
Run appropriate tests/checks
 ↓
Fix failures
 ↓
Re-run verification
 ↓
Commit only when verification passes
```

Do not commit known-broken work merely because the code compiles partially.

Tests should cover:

- domain logic
- state transformations
- protocol encoding/decoding
- configuration validation
- color/ambient calculations
- ViewModel state
- repository behavior where practical
- UI behavior where practical

Platform-specific functionality must be verified on the relevant platform when available.

---

## 26. Git Rules

Keep commits small and logical.

Preferred sequence:

```text
task
→ verify
→ commit
→ next task
```

Commit messages should describe the completed change.

Do not mix unrelated refactors with feature work unless required.

Never commit:

- secrets
- private keys
- API keys
- generated local IDE state
- machine-specific files
- build output

---

## 27. Codex / Agent Workflow

Before changing code:

1. Read `AGENTS.md`.
2. Read `PROJECT_STATUS.md`.
3. Inspect existing architecture and relevant files.
4. Identify dependencies of the requested change.
5. Make the smallest coherent change.
6. Run verification.
7. Update `PROJECT_STATUS.md`.
8. Commit after successful verification.

Do not rewrite large parts of the project without a clear architectural reason.

Do not introduce new libraries when existing project dependencies can solve the problem cleanly.

Do not invent protocol details. If hardware/app protocol is not yet specified, mark it as pending rather than guessing.

---

## 28. Current Scope

### V1

- KMP/CMP foundation
- Android/iOS/Windows/Linux/macOS targets
- Device abstraction
- BLE/transport abstraction
- Device discovery/connection
- Device configuration
- Dynamic line count
- Dynamic LED count per line
- Dashboard
- Dynamic effects
- Runtime LED controls
- LED preview
- Dynamic accent
- Ambient UI
- Settings
- Hardware configuration
- local persistence as required

### Future

Only these future product features are currently reserved:

- Scheduler
- Music Reactive

Do not reintroduce previously removed roadmap items such as:

- WiFi
- OTA
- Presets
- MQTT
- Web UI
- IR Remote

unless the project owner explicitly changes the roadmap.

---

## 29. Non-Negotiable Rules

1. Do not hard-code the number of LED lines.
2. Do not hard-code LED count.
3. Do not couple domain logic to ESP32.
4. Do not couple UI to raw BLE packets.
5. Do not treat sent commands as confirmed state.
6. Do not make Settings visually noisy.
7. Do not make ambient animation distracting.
8. Do not hard-code a single effect parameter model.
9. Do not break other platforms to fix one platform without a deliberate platform-specific boundary.
10. Test after each meaningful task.
11. Commit only after successful verification.
12. Update `PROJECT_STATUS.md` after meaningful progress.
13. Do not invent unspecified hardware/protocol behavior.
