# NetScan Android

NetScan is an offline-first Android application designed for local network reconnaissance, Wi-Fi environment auditing and diagnostics. It combines low-level socket operations, hardware-backed encryption and custom Compose Canvas visualizers into a single toolkit that runs entirely on-device without phoning home or relying on external cloud APIs.

## Why This Exists

Most mobile network scanners either bury basic metrics behind paywalls or route discovery data through remote telemetry servers. NetScan was built to be completely self-contained. All ARP parsing, packet crafting, spectrum analysis and PDF generation happen locally on your phone with zero internet access required for core functionality.

## Core Capabilities

### Local Network Discovery
- Subnet sweep: Discovers active hosts across IPv4 subnets using concurrent socket probes and system ARP table inspection.
- Service discovery: Identifies UPnP and SSDP devices alongside mDNS services broadcast via Android Network Service Discovery (NsdManager).
- Port scanner: Scans standard, registered or custom TCP port ranges with configurable socket timeouts and thread concurrency.
- Banner grabber: Connects to open ports to capture initial response headers and daemon strings (SSH, HTTP, FTP and RTSP).
- Vendor OUI lookup: Resolves MAC prefixes to hardware manufacturers using an embedded offline database.

### Wi-Fi Reconnaissance and Signal Analysis
- Access point monitor: Scans nearby 2.4 GHz, 5 GHz and 6 GHz networks to report BSSID, channel bandwidth, RSSI and security capabilities (WEP, WPA2, WPA3 and OWE).
- Kalman filter smoothing: Filters raw RSSI fluctuations through a 1D Kalman state estimator to provide stable signal tracking without erratic noise.
- Spectrum overlap visualizer: Renders custom Canvas frequency curves showing channel crowding across both 2.4 GHz and 5 GHz bands.
- Floorplan heatmapper: Maps Wi-Fi signal propagation across a user-supplied floorplan image using Inverse Distance Weighting (IDW) interpolation.

### Active Intervention Tools
- Wake-on-LAN (WoL): Constructs standard magic packets (6 bytes of 0xFF followed by the target MAC repeated 16 times) and broadcasts them over UDP ports 7 and 9 to wake sleeping machines.
- Interactive socket probe: Sends custom raw text or HEX payloads to user-specified TCP ports and displays live socket responses.
- Scan profiles: Three preset operational profiles (Stealth, Balanced and Aggressive) to control concurrency limits, socket connection delays and timeout windows.

### Security and Protocol Audits
- ARP spoof detection: Watches the local ARP cache for duplicate MAC entries or gateway address collisions.
- TLS certificate auditor: Inspects SSL/TLS certificates on target web interfaces, checking cipher strength, validity dates and issuer trust chains.
- DNS integrity check: Compares resolution results between local DHCP-assigned resolvers and trusted public providers to spot DNS poisoning.
- Bufferbloat and latency profiler: Measures unloaded versus loaded latency under active TCP/UDP traffic to evaluate router queue management.

### Offline PDF Reporting
- Direct-to-PDF engine: Uses Android native PdfDocument API to build paginated audit summaries with vector host tables, security scores and visual charts.
- Zero web dependencies: Reports are compiled, formatted and saved locally to device storage without third-party cloud rendering services.

## Security Architecture

Local network audit data can expose sensitive information about home or enterprise infrastructure. NetScan uses multiple layers of device security:

- Encrypted database: Persistence is handled by Room backed by SQLCipher using AES-256 GCM.
- Android KeyStore: Database encryption passphrases are generated at install time and stored in hardware-backed KeyStore storage using master keys protected by StrongBox where hardware permits.
- Biometric gatekeeper: Access to stored audit reports and database snapshots can be locked behind BiometricPrompt, with fallback to system PIN or pattern.
- Ephemeral audit mode: A toggleable private mode that bypasses database writes completely, holding scan results in memory for the active session only.
- Window protection: Sensitive audit views apply FLAG_SECURE to prevent the Android OS from saving screenshots or exposing screen contents in the recent apps switcher.

## Architecture and Tech Stack

The codebase adheres to modern Android architecture principles with clean separation between data sources, domain services and presentation state:

- UI: 100% Jetpack Compose using Material 3 and adaptive layout components.
- Navigation: Jetpack Compose Navigation with Kotlin Serialization type-safe routes. It uses NavigationSuiteScaffold to render a bottom bar on standard phones and automatically switch to a navigation rail on foldables and tablets.
- Asynchronous runtime: Kotlin Coroutines and StateFlow for managing concurrent socket sweeps and real-time UI state streams.
- Local storage: Room with SQLCipher integration and AndroidX EncryptedSharedPreferences for configuration flags.
- Visualizations: Hardware-accelerated custom Canvas implementations for the dynamic force-directed topology graph, frequency spectrum curves and IDW heatmap.
- Minification and performance: Baseline Profiles for fast startup alongside tuned ProGuard rules to protect reflection-dependent serialization while stripping debug logging in release builds.

## Project Structure

```
app/src/main/java/com/example/networkscanner/
├── data/
│   ├── local/          # Room database entities, DAOs and SQLCipher configuration
│   └── repository/     # Network scan repositories and data layer coordinators
├── di/                 # Dependency injection modules
├── domain/
│   ├── model/          # Core domain models, host entities and scan profiles
│   └── service/        # Low-level network engines (WoL, ARP, SSDP, TLS and DNS)
├── ui/
│   ├── components/     # Custom Canvas charts, bottom sheets and dialogs
│   ├── navigation/     # Adaptive NavigationSuiteScaffold and type-safe routes
│   ├── screens/        # Compose screens (Dashboard, LAN, Wi-Fi, Health and Reports)
│   ├── state/          # UI state definitions
│   ├── theme/          # Material 3 colors, typography and themes
│   └── viewmodel/      # Architecture ViewModels managing state flows
└── util/               # Biometric managers, report exporters and math utilities
```

## Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2) or newer
- Android SDK 34
- JDK 17 (recommended for Gradle 8.2 compatibility)
- Physical Android device running Android 10 (API level 29) or higher (recommended for Wi-Fi scanning and raw socket operations, as emulators cannot access physical Wi-Fi hardware)

### Building From Source

1. Clone the repository:
   ```bash
   git clone git@github.com:Senorpossum/NetScanAndroid-WIP-.git
   cd NetScanAndroid-WIP-
   ```

2. Open the project in Android Studio.

3. Verify that Android Studio is configured to use JDK 17:
   - Navigate to **Settings** > **Build, Execution, Deployment** > **Build Tools** > **Gradle**.
   - Set **Gradle JDK** to version 17.

4. Build and install the debug build on your connected device:
   ```bash
   ./gradlew installDebug
   ```

### Required Permissions
The app requests several permissions required by the Android operating system to perform network analysis:
- `NEARBY_WIFI_DEVICES` and `ACCESS_FINE_LOCATION`: Required by Android to scan for nearby Wi-Fi access points and read RSSI metrics.
- `INTERNET` and `ACCESS_NETWORK_STATE`: Required to open socket connections, perform port sweeps and monitor connection changes.
- `USE_BIOMETRIC`: Used to authenticate before decrypting saved audit reports.

## Contributing

Pull requests are welcome. For major architectural additions or changes to low-level socket handling, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.
