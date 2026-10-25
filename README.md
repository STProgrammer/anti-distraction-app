# Intent

A native, offline Android companion for intentional social media use. Kotlin, Jetpack Compose, Material 3, Android SQLite, and an accessibility service. Designed for the Galaxy A36 running Android 16.

## Use

Install `dist/Intent-1.0.0.apk`, open Intent, and follow its three-step setup. Allow installation from the specific file manager opening the APK if Android asks. Android's restricted-settings protections remain in force.

1. Allow **Usage Access** for Intent.
2. Enable **Settings → Accessibility → Installed apps → Intent**. If Android blocks this, open Intent's App info and use **Allow restricted settings** only if Android offers it and you trust the APK, then return to Accessibility. An organization policy may prevent this.
3. Under **Settings → Apps → Intent → Battery**, choose **Unrestricted** if available. Under **Settings → Battery → Background usage limits**, add Intent to **Never sleeping apps** / **Never auto sleeping apps** and remove it from sleeping lists. Samsung labels vary by firmware and region.
4. Choose monitored apps and adjust the timer position in Settings if needed.

No account, internet, notification, separate overlay, storage, location, microphone, or camera permission is requested. Accessibility overlays supply the warning and pass-through timer. There is no background foreground-service notification to dismiss.

## Behavior

- Nine default social apps; installed launcher apps are searchable by actual package and label, with their icons. Missing defaults remain manageable and take effect if installed later.
- Each monitored app needs a single intention on a new session. Other requires no text. There are no limits, lockouts, or additional prompts within an uninterrupted session.
- Home, Back, and the Leave button exit a warning safely. Keyboards and duplicate events preserve approval. System interruptions pause counting; returning to the same app resumes it. Locking ends the session.
- A compact, non-touchable timer shows the combined daily total. Its vertical position is adjustable; it follows system insets and orientation.
- Daily totals, 7/30-day charts, app selections, and editable/reorderable answers remain local and survive updates/reboots. At least one nonblank answer is required.
- Both themes follow Android. The warning remains black and red.

## Architecture

`core/SessionMachine.kt` owns one current app and one session approval. It is independent of Android. `core/UsageMath.kt` splits elapsed intervals into local dates, including daylight-saving transitions.

`monitor/MonitorService.kt` observes accessibility window transitions. It inspects only window IDs, type, focus, picture-in-picture state, and package identifiers. It never calls event text/source, window root, or content-node APIs. Android requires the general window-content capability to expose window metadata; the setup disclosure explains that bundled permission. No screen contents are collected. App identifiers are retained only for selected apps and the current recovery journal; unrelated transition metadata is transient.

`monitor/UsageSource.kt` supplies a secondary activity lifecycle signal and crash recovery. It reads only package, timestamp, and event type. `monitor/Overlays.kt` owns the warning's Compose lifecycle and the non-focusable, non-touchable timer window. Overlays are removed on departure, screen lock, disabling, interruption, or service teardown.

`data/Store.kt` uses a versioned SQLite database with WAL. Committed daily totals and the outstanding session checkpoint are updated in one transaction. Duration uses elapsed realtime; wall time assigns calendar days. There is one accounting owner, so simultaneous apps cannot be counted twice. Checkpoints run every five seconds while active and on transitions; timer redraws do not accrue usage themselves. Clock/timezone changes close the old interval before starting a new one. Previously recorded dates are not rewritten after a timezone change.

`ui/` contains the dashboard, charts, app picker, intention editor, setup, and settings. State comes from a small repository and lifecycle-aware flows. There is no DI, remote SDK, analytics, chart dependency, or network stack.

## Supported limitations

- Android reports foreground transitions after an app begins opening. A brief glimpse or very fast interaction before the warning is possible; this is an intention aid, not a security barrier.
- Android or Samsung can stop/disable an accessibility service. No third-party app can promise uninterrupted monitoring. The dashboard distinguishes permission enabled from service connected. Android normally binds enabled services again after boot/unlock; Intent does not bypass restricted settings or force-stop behavior.
- Recovery uses the outstanding approved session and Android's activity/lock/shutdown events. It stops at the first confirmed interruption. Later usage while the service was unavailable cannot reliably distinguish a visible warning from active use, so it is not guessed. A local notice flags uncertain gaps. If events are unavailable, the committed total is retained; the last uncommitted interval may be lost (normally at most five seconds). Complete accounting while the service is disabled cannot be guaranteed.
- Foreground focus determines the single counted app in split-screen. Background audio and unfocused picture-in-picture are not counted. OEM window reporting can differ. A system dialog belonging to the same app preserves its session; a system-owned blocking surface pauses it.
- A visible bar necessarily covers a small area. Touches pass through; the vertical adjustment and visibility switch accommodate different app controls. Android may suppress overlays on protected system surfaces.
- Usage begins when Intent is set up; past device usage is not imported. Removing a monitored app preserves earlier combined totals. Uninstalling Intent removes its private data; cloud backup is disabled.

## Build and signing

Requirements: JDK 17 or newer, Android SDK 36, Gradle wrapper 8.13. The pinned AGP 8.13.2/Kotlin 2.2.10/Compose BOM 2025.05.01 toolchain builds API 36 without preview SDKs. Minimum Android is 12 (API 31).

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
```

`local.properties` points to the local Android SDK. Personal signing material is in ignored `signing/intent-release.jks` and `signing/local.properties`. Preserve both privately for future updates; neither belongs in source control or APK distribution. `signing/local.properties` contains `storePassword=...` and `alias=intent`. Both debug and release builds use the same personal signer so verification builds can be replaced without losing settings. On another machine, supply the same signing material to build compatible updates.

The `probe` module contains two small local test apps. They are never bundled in Intent. App instrumentation tests use isolated databases. External service tests run from the probe process so Android does not stop Intent's service when instrumentation starts. They require the two probe apps to be selected in Intent and add real test usage; use a clean test installation.

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :probe:installOneDebug :probe:installTwoDebug :probe:connectedOneDebugAndroidTest
```

Service integration requires normal Android permission grants first; it fails if monitoring is unavailable. Re-enable the service after app-process instrumentation if Android stops it. See `docs/VERIFICATION.md` for actual results and remaining device checks.

## Platform references

- [Android accessibility service and window metadata](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Android overlay and pass-through window flags](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
- [Android usage lifecycle events](https://developer.android.com/reference/android/app/usage/UsageEvents.Event)
- [Samsung background app management](https://developer.samsung.com/mobile/app-management.html)
- [AGP 8.13 compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [UpScrolled's package listing](https://play.google.com/store/apps/details?id=com.upscrolled.app)
