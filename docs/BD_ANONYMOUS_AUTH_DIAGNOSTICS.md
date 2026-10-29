# Anonymous authentication diagnostics

Implemented for 17.22 (57), 28 October 2026. Extended to Global on 29 October 2026. Applies to method `anonymous` in both
Bangladesh and Global. Other providers retain their existing event payloads and
Crashlytics behaviour.

## Event contract

Exactly one `sign_up_error` is emitted at the existing failure source. Parameters:
`method=anonymous`, `stage=<controlled step>`, `step=<controlled step>`,
`outcome=failure`, `reason=<controlled category>`, `code=<allow-listed code>`.
Steps/stages: `authentication`, `profile_read`, `profile_write`, `missing_user`,
otherwise `unknown`. Post-authentication profile errors are not labeled as Auth
request errors. No new authentication-error event is introduced.

## Exact classification

Type mappings take precedence:

| Input | code | reason |
|---|---|---|
| FirebaseNetworkException | NETWORK_ERROR | network |
| FirebaseTooManyRequestsException | TOO_MANY_REQUESTS | too_many_requests |
| FirebaseApiNotAvailableException | API_NOT_AVAILABLE | api_not_available |

For FirebaseAuthException, retain its exact SDK code ONLY if listed below:

| SDK code | reason |
|---|---|
| ERROR_NETWORK_REQUEST_FAILED | network |
| ERROR_TOO_MANY_REQUESTS | too_many_requests |
| ERROR_QUOTA_EXCEEDED | quota_exceeded |
| ERROR_OPERATION_NOT_ALLOWED | operation_not_allowed |
| ERROR_APP_NOT_AUTHORIZED | app_not_authorized |
| ERROR_API_NOT_AVAILABLE | api_not_available |
| ERROR_API_NOT_AVAILABLE_WITHOUT_GOOGLE_PLAY | api_not_available |
| ERROR_INTERNAL_ERROR | internal |
| ERROR_WEB_INTERNAL_ERROR | internal |

The following recognized codes are preserved with `reason=provider_error`:
`ERROR_INVALID_CREDENTIAL`, `ERROR_INVALID_USER_TOKEN`, `ERROR_USER_TOKEN_EXPIRED`,
`ERROR_USER_DISABLED`, `ERROR_USER_NOT_FOUND`, `ERROR_REQUIRES_RECENT_LOGIN`,
`ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL`, `ERROR_CREDENTIAL_ALREADY_IN_USE`,
`ERROR_INVALID_EMAIL`, `ERROR_WRONG_PASSWORD`, `ERROR_EMAIL_ALREADY_IN_USE`,
`ERROR_WEAK_PASSWORD`, `ERROR_ADMIN_RESTRICTED_OPERATION`, `ERROR_TENANT_ID_MISMATCH`,
`ERROR_UNSUPPORTED_TENANT_OPERATION`.

For FirebaseFirestoreException, code is `FIRESTORE_` plus the fixed SDK Code enum
name. Reasons: UNAVAILABLE/DEADLINE_EXCEEDED -> `network`, PERMISSION_DENIED ->
`permission_denied`, UNAUTHENTICATED -> `unauthenticated`, RESOURCE_EXHAUSTED ->
`resource_exhausted`, INTERNAL -> `internal`, UNKNOWN -> `unknown`, remaining SDK
enum members -> `provider_error`. The step/stage retains profile_read/profile_write.

Null error at missing_user -> `MISSING_USER` / `unknown`. Any other unrecognized
error, unrecognized SDK string or absent information -> `UNKNOWN` / `unknown`.
No message/cause parsing and no arbitrary exception class names are used.

`ERROR_QUOTA_EXCEEDED` is NOT evidence of anonymous account-creation IP limits:
Google documents that code for phone verification. `too_many_requests` remains a
generic Firebase throttle indication. No App Check/Play Integrity cause is inferred
from generic errors. Relevant configuration is not modified.

## Privacy and behaviour

This path bypasses construction/reporting of AuthFailureException and does not
call Crashlytics.recordException. It writes a controlled breadcrumb with the same
six diagnostic fields. Anonymous logcat messages contain no exception message,
UID or original stack. Existing UI messages/callbacks are preserved; UI display is
not a new telemetry field. No raw throwable/cause/stack is attached to diagnostics.
Uncaught programming defects still follow normal application crash handling.

Firebase signInAnonymously, its single request, existing Auth success events,
Firestore server read/merge, FCM initialization, callback and pending login_success
gate are unchanged. No Auth retry, provider, onboarding or account change.
The consent-recovery work already present in the working tree is a separate task.

## PROD GA4 configuration — verified in UI

Property 503154947 (`soccer-prod-1754048346`), all event scope. Before creating
each definition the full existing list was checked for its parameter; no duplicate
was created. The existing reason definition was not modified.

| Display name | Event parameter | State on 2026-10-28 |
|---|---|---|
| Auth method | method | created |
| Funnel stage | stage | created |
| Auth step | step | created |
| Funnel outcome | outcome | created |
| Auth error code | code | created |
| Signup decline reason | reason | existing, preserved |

Final UI lists exactly six definitions (1-6 of 6). Screenshot evidence:
C:/Users/Dom/.codex/visualizations/2026/10/19/01a1555d-eec9-7072-a0a7-d75845d5ebab/ga4-auth-custom-definitions.png
Registration is configuration, not retroactive reclassification. The two historical
provider_error events are not reinterpreted. Existing other-provider code
parameters retain legacy values; the code allow-list now applies to anonymous in both flavours.

## Verification

Focused suites: AnonymousAuthDiagnosticTest, AnonymousAuthReportingTest,
AnonymousLoginPresentationTest, AnalyticsFunnelTest. Passed 26 tests for DEV BD
and 26 for DEV Global, zero failures/errors. Coverage includes known mappings,
unknown exceptions/codes, no message parsing, privacy, a single failure event,
Auth versus profile failures, unchanged SDK success/profile/callback behaviour,
the same safe anonymous diagnostics in Global and BD, and unchanged other-provider diagnostics.
DEV Bangladesh Debug and DEV Global Debug APK builds passed.
PROD Bangladesh Release and PROD Global Release APK builds passed (BUILD SUCCESSFUL).
No store upload, installation or production authentication test performed.

## References

- https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuthException
- https://firebase.google.com/docs/reference/android/com/google/firebase/FirebaseNetworkException
- https://firebase.google.com/docs/reference/android/com/google/firebase/FirebaseTooManyRequestsException
- https://firebase.google.com/docs/reference/android/com/google/firebase/FirebaseApiNotAvailableException
- https://firebase.google.com/docs/auth/limits
- https://support.google.com/analytics/answer/14240153
