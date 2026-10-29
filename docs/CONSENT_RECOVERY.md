# Recovering startup consent after network errors

Prepared for 17.22, shared by Global and Bangladesh.

The DEV phone tests showed that UMP startup failure let the menu continue but
prevented another consent request until process restart. Automatic `first_open`
was still uploaded after connectivity returned. This change improves consent
recovery; it is not a demonstrated fix for the Ads/GA4 install discrepancy.

`startupConsentFinished` continues to mean that the menu may proceed, including
offline. A separate pending-retry state records network/time-out failures during
consent-info refresh, form loading or form presentation. Consent is never granted
by an error/retry, and an SDK success still uses the existing consent policy.

Retries are limited to three per application process, after minimum delays of
5, 15 and 45 seconds from the preceding failure. There are no timer-driven
requests while offline. Network availability/capability changes and menu focus
can resume a pending retry after the delay. Exhausting the budget waits for the
next process start; invalid-operation/internal errors do not retry automatically.

The menu must be resumed, attached and focused. A delayed response must recheck
window readiness before loading/showing a retry form. A stopped menu cancels its
timer; the timer only holds a weak reference to its activity. Concurrent signals
cannot overlap UMP requests. A successful retry does not repeat the menu's initial
startup continuation. Completed consent, including rejection, does not retry.

Verification covers same-process network recovery, the backoff/attempt limit,
duplicate signals, offline timers, stopped/unfocused hosts, a response arriving
after focus loss, invalid operation, timeouts and consent-form load failure.
Existing consent tests cover preserving rejection and partial consent.

Phone regression: install DEV without resetting the current account if it is
needed; for a genuinely fresh consent run use a disposable DEV installation.
Launch offline, reach the menu, then enable networking without killing the app.
Expect a consent request/form on the active menu once the retry delay has passed.
Repeat with the app backgrounded: no form should open until the menu is active.
Reject consent and verify that it stays rejected without a new retry loop.
No custom `first_open` or Analytics reset is introduced.

Automated result (2026-10-28): the focused startup/retry/consent/menu regression
suite passed 30/30 tests for DEV Bangladesh and 30/30 for DEV Global. An earlier
command unintentionally ran the entire BD suite: 290 tests, 48 failures and one
skip, including unsupported SDK 37 Robolectric initialization and unrelated
assertion failures. These broader failures were not resolved or certified as
baseline by this task; the successful focused suite is not a full-suite pass.
Phone verification passed on DEV Bangladesh 17.22-BD (2026-10-28): initial
startup without networking failed UMP at 13:50:06 local. After connectivity
returned, the same process (PID unchanged) started retry 1 at 15:24:16, refreshed
consent info and loaded the form at 15:24:20. Before the decision, storage consent
remained G100 (both denied). The user rejected all at 15:25:26; UMP reported
OBTAINED (a completed decision, not permission), storage remained G100 and
ad_user_data remained denied. At 15:27:14, no further request/retry was logged
and the user confirmed no repeated form. Background/focus and retry-limit cases
are covered by automated tests; they have not yet been repeated manually.