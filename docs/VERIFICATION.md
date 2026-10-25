# Verification record

Device: connected Samsung Galaxy A36, model **SM-A366B**, Android **16 / API 36**. Checks performed on 25 October 2026. One UI 8.5 is the user's supplied firmware description; Android/model were also checked through ADB.

## Automated checks

- **16 JVM tests**: first-open gating, repeated events, rapid switching, separate app approvals, Home/return, temporary interruptions, lock/disable transitions, combined totals, second formatting, midnight boundaries, multiple days, backwards intervals, and Oslo's 23/25-hour daylight-saving days.
- **4 on-device SQLite tests**: settings and answer ordering across reopening, rejecting an empty answer list, atomic/idempotent recovery spanning midnight, persistent daily totals, duplicate starts, and stopped timer behavior.
- **3 on-device Compose tests**: disabled Continue until selection, exclusive selection, Other without text, exit action, answer add/edit/delete/reorder, and reachable warning actions at 160% font scale.
- **4 external on-device service tests**: first-open warning; cross-app prompts; combined accounting excluding five seconds spent on warnings; keyboard and app-dialog continuity; notification-shade pause/resume; Home/Back/Leave/Recents; global disable/re-enable; timer visibility; landscape warning and portrait return; service recovery after terminating its process. The main interaction, accounting/rotation, and Recents scenarios also passed against the signed release build. Process termination used the debuggable build's private process access.

All **27 distinct automated tests passed** (16 JVM, 11 on the phone). The revised warning also passed all three Compose tests again. Revoking and restoring Usage Access was checked through Samsung Settings: overlays were absent while access was denied. Light and dark screens were visually inspected and the original system theme was restored.

The monitoring tests use two local, network-free fixture apps. No personal social account was opened or interacted with. Test applications and test-only usage are removed before handoff.

The final signed APK is installed on the connected phone. Usage Access and the accessibility service are enabled, Battery is set to **Unrestricted**, and the dashboard reports monitoring ready for the six installed defaults. The other default selections remain listed as missing. The clean history contains no fixture usage. Samsung's optional Never sleeping list was not changed.

The actual warning, timer, onboarding, and dashboard were visually inspected on the phone. Layout fixes from that inspection preserve Samsung system navigation and accommodate landscape/cutouts. Screenshots are in `docs/screenshots/`; screenshots taken during verification can contain fixture names and test usage.

## Build and security checks

- Debug and signed, minified release builds succeed.
- Android Lint reports no errors. Remaining warnings concern newer tool/library versions, optional Kotlin convenience APIs, and targeting Android 16 rather than the newer preview/next platform.
- The main APK has no internet permission. Its only requested special access is Usage Access; the service is protected by Android's accessibility binding permission. Window-content capability is declared because Android requires it for window metadata, but production code never retrieves content nodes or event text.
- Cloud backup and device transfer are excluded. The database is private to the app. APK signature validation and a distribution SHA-256 accompany the final package.

## Remaining phone checks

These have not been exhaustively verified and are not implied by the automated results:

1. Reboot, unlock normally, and confirm Intent reports connected before opening a selected app.
2. Lock during an approved session, wait a minute, unlock, and confirm locked time is excluded and the next session asks again. Pure session logic is tested; secure-lock UI is left for the phone owner.
3. Leave the phone idle for several hours with Samsung battery policies enabled, then confirm monitoring resumes. Long-term Doze/background stress and battery profiling were not performed.
4. Try split-screen and picture-in-picture in the social apps you actually use. Android/OEM focus reporting differs; Intent follows one foreground app and excludes background playback.
5. Check any app-specific protected screen, unusual third-party keyboard, and very rapid launcher/Recents gestures you regularly use.

Midnight and daylight-saving intervals are tested with deterministic timestamps; the phone's clock was not changed. The dashboard/history also passed through the phone's actual midnight during verification and showed the new day at zero with the previous total retained. An active session spanning that real midnight was not manually timed. No monitoring aid can promise a warning before Android reports the launch or complete records while its service is disabled. Uncertain recovery gaps are reported, never silently represented as verified usage.
