# Fix build and missing components

The project currently fails to build due to missing resources, missing Compose theme components, and Gradle memory issues. This plan addresses these to get the project into a building state.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///home/evan/.gemini/antigravity-ide/scratch/android%20app/gradle.properties)
- Increase Gradle heap size to 4GB to prevent `OutOfMemoryError` during dex merging.

### Android Resources

#### [NEW] [themes.xml](file:///home/evan/.gemini/antigravity-ide/scratch/android%20app/app/src/main/res/values/themes.xml)
- Define `Theme.NetworkScanner` inheriting from `Theme.Material3.DayNight.NoActionBar`.

#### [NEW] [ic_launcher.xml](file:///home/evan/.gemini/antigravity-ide/scratch/android%20app/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml)
- Create a basic adaptive icon to satisfy AAPT requirements.

#### [NEW] [ic_launcher_round.xml](file:///home/evan/.gemini/antigravity-ide/scratch/android%20app/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml)
- Create a basic adaptive icon for round icons.

### Compose Theme

#### [NEW] [Color.kt](file:///home/evan/.gemini/antigravity-ide/scratch/android%20app/app/src/main/java/com/example/networkscanner/ui/theme/Color.kt)
- Define basic Material 3 color palette.

#### [NEW] [Type.kt](file:///home/evan/.gemini/antigravity-ide/scratch/android%20app/app/src/main/java/com/example/networkscanner/ui/theme/Type.kt)
- Define default Typography for the theme.

#### [NEW] [Theme.kt](file:///home/evan/.gemini/antigravity-ide/scratch/android%20app/app/src/main/java/com/example/networkscanner/ui/theme/Theme.kt)
- Implement `NetworkScannerTheme` composable as expected by `MainActivity`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify the project builds successfully.
