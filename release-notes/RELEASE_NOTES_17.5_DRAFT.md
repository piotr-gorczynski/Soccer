# Release 17.5 – Major updates since 17.4

Baseline release: **17.4** published on **February 4, 2026 at 23:10**.
Review window used: merged pull requests from **2026-02-04 23:10 UTC** through **2026-04-30**.

## 1) Bangladesh migration & app split hardening
- Introduced Bangladesh-specific migration strategy and implementation foundations.
- Added/iterated uninstall flow for coexistence handling between Global and Bangladesh apps (multiple PRs from #1200 to #1229), including:
  - reliable uninstall prompts,
  - Android 11+ package visibility support,
  - bridge activity/task behavior fixes,
  - prevention of prompt loops and dialog race conditions.
- Added Bangladesh flavor branding differentiation (name/icon behavior) and localization updates.
- Added a global feature flag `BANGLADESH_PROMO_ENABLED` and Bangladesh promotion dialog behavior updates.

Key PRs: #1191, #1193, #1200, #1202, #1204, #1206, #1208, #1210, #1212, #1213, #1214, #1215, #1216, #1217, #1219, #1221, #1223, #1225, #1227, #1229, #1231, #1233, #1235, #1238, #1240.

## 2) Payments & winner payout flow (backend)
- Extended Firestore schema for Bangladesh-related payment workflows.
- Allowed winners to submit recipient info on payment records.
- Added new Cloud Functions for tournament completion and payment status updates:
  - `onTournamentComplete`
  - `updatePaymentStatus`
- Added Cloud Build deploy YAMLs and Firebase deploy filter/codebase fixes to stabilize deployment of new functions.

Key PRs: #1246, #1248, #1251, #1252.

## 3) Stability & crash fixes
- Fixed `RemoteServiceException` broadcast delivery crash.
- Improved Bangladesh country detection fallback behavior.

Key PRs: #1250, #1198.

## 4) Security, dependencies, and build modernization
- Resolved high-severity npm audit vulnerability.
- Multiple dependency and lockfile updates.
- Updated Android Gradle plugin to **9.1.1**.
- Additional build/deprecation warning cleanups across Android/Firebase auth/Gradle areas.

Key PRs: #1236 and several dependency/build maintenance commits in the same period.

## 5) Tournament/live-ops configuration updates
- Updated tournament schedules/names for events such as Spring Warm-Up Showdown, Early Summer Clash, and May Bloom Clash.

Key PR: #1189 and subsequent tournament-config update commits.

---

## Suggested external release narrative (English)
This release focuses on **Bangladesh migration readiness**, **more reliable uninstall/coexistence UX between app variants**, and **backend payout automation**. It also includes a **production crash fix**, **security/dependency upgrades**, and **ongoing tournament configuration updates** to support upcoming events.
