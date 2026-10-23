# Authentication failure diagnostics (17.17)

New authentication failures are recorded as non-fatal `AuthFailureException`
reports in Crashlytics for both global and Bangladesh. Existing collection and
consent settings still apply. This cannot recover failures from older versions.

Each report includes the attempted provider, operation stage, SDK error code
(or exception class), application ID, version, build type and environment.
The original stack trace is retained, but SDK messages and exception causes are
not attached: they may include email addresses, tokens or other personal data.
No credentials or entered form values are added to these diagnostics.

Stages distinguish provider UI, Firebase authentication/credential exchange,
profile reads/writes, registration, verification email and password reset.
User cancellation produces `auth_cancelled` in Analytics without a non-fatal.
Other failures produce `sign_up_error` with `method`, `step` and `code`.
The UI callback does not send a second copy of the same failure.

## Verification before release

On a DEV build on a test device, try an invalid email/password login, cancel
Google/Facebook sign-in, and try an unverified email account. After reopening
the app, inspect non-fatal reports in the matching Firebase DEV application.
Verify provider/stage/code and absence of entered values, and verify cancellation
is absent from the non-fatal list. Repeat a successful login as a regression check.
Do not deliberately crash or change production accounts to test this feature.

Analytics event counts are not account counts: the existing `sign_up` event is
also emitted by successful returning-provider logins. These diagnostics do not
change that historical event definition or identify the cause of past failures.

## Successful sign-ins (17.18)

For the subsequent Bangladesh schema-2 recommended `login`/`sign_up` definitions,
privacy fixes and full funnel audit, see [Bangladesh analytics funnel](BANGLADESH_ANALYTICS_FUNNEL.md).

The submitted 17.18 build emits `login_success` after authentication, before
Terms/nickname completion. In the subsequent source change, an explicit successful
sign-in creates a pending attempt instead. Menu emits `login_success` only after
server-confirmed acceptance of the required variant-specific Terms and a nonblank
nickname read from the server. This covers OAuth providers, Facebook token exchange,
verified email/password, and anonymous sign-in. It is not emitted on screen open,
failure, cancellation, unverified email, account creation awaiting verification,
or merely restoring an existing session on app launch. A pending attempt survives
process recreation while completing onboarding; completion consumes it once.
An existing account with Terms and nickname already in place counts after those
checks succeed. Failed/offline checks do not count until a later successful check.
The original provider is retained across Terms/nickname screens; account changes
and stale callbacks cannot complete another attempt. This source change requires
a new build and does not alter the already submitted 17.18 artifacts.

The `method` parameter is `google`, `facebook`, `microsoft`, `email`, or `anonymous`.
Build/market metadata uses the existing default Analytics parameters. This event
does not include user-entered values. Repeat successful sign-ins generate repeat
events; the event count is not a count of new accounts or unique people.
No Google Ads conversion or bidding configuration is changed by this code.
