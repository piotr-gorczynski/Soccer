# Analytics account correlation

`AnalyticsUserIdentity` uses `FirebaseAnalytics.setUserId` in Global and Bangladesh.
It adds no account-ID event parameter, custom dimension or user property.
The same derived value is assigned using `FirebaseCrashlytics.setUserId` while
Crashlytics collection is enabled. Sign-out clears the Crashlytics ID with an
empty string; Analytics uses null. The two collection policies remain separate:
Analytics consent denial does not disable otherwise-enabled crash diagnostics.
Identity synchronization does not enable either SDK's collection.

The value is `u1_` plus lowercase SHA-256 hex of UTF-8 text:

`soccer-analytics-v1:<BuildConfig.FLAVOR_environment>:<Firebase Auth UID>`

Use the exact generated environment value (for example `_prod`). Global and
Bangladesh share the mapping in the same environment; DEV and PROD are separate.
Authorized tooling can recompute it from a UID without a Firestore mapping.
This is pseudonymization, not anonymization. No emails, nicknames, provider IDs
or device IDs are used.

## Lifecycle

The application Auth listener synchronizes restored sessions, anonymous login,
account switching and sign-out. Auth success callbacks synchronize before
login/sign_up events so nickname and Terms events inherit the ID as well.
Identity is assigned only when the existing UMP-based Analytics consent policy
is resolved and permits Analytics. Consent refresh also synchronizes identity;
withdrawal, unresolved consent and logout clear the SDK ID using null.
No consent choices are changed; accepting Terms does not grant Analytics consent.
Earlier events are not retroactively linked, and no duplicate events are added.

## Reporting and publication

Use the Analytics BigQuery event export's `user_id` for account-level queries.
Aggregate GA4 reports are not a direct Firestore UID search interface. In
Crashlytics, search reports by the derived ID to inspect associated errors and
breadcrumbs. This code does not enable exports, modify Google Ads or upload
standalone histories of error-free sessions. Existing reports are not relabeled.

The local privacy notices were updated on 25 October 2026 in all existing
translations (20 Global and English/Bengali Bangladesh), including the English
Global alias. They disclose account-linked Analytics and Crashlytics and their
purposes. Publish these Hosting sources before releasing account correlation,
and verify Play Data safety declarations cover user identifiers and Analytics
activity. Include the derived
ID in applicable retention/deletion procedures: clearing it does not erase past
events or exports. Updating local sources does not publish privacy notices or
edit Play declarations.

Manual test: allow Analytics, authenticate anonymously, inspect SDK debug
User-ID before nickname setup, complete onboarding and restart (same ID), sign
out (cleared), switch account (different ID), then deny Analytics (cleared).
For Crashlytics, verify the same derived ID on a deliberately reported DEV
nonfatal error; logout should clear it. Analytics denial must leave the
Crashlytics ID intact if crash collection remains enabled. Clearing IDs does
not delete earlier crash reports; follow the applicable deletion process.
Avoid exposing account identifiers in shared screenshots.

Official references checked 25 October 2026:

- https://firebase.google.com/docs/analytics/userid?platform=android
- https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
- https://developers.google.com/analytics/policy
- https://firebase.google.com/docs/android/play-data-disclosure
- https://support.google.com/googleplay/android-developer/answer/10144311
