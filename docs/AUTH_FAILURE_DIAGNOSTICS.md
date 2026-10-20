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
