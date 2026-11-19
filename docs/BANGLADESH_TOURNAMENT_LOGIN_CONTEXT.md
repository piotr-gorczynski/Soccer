# 17.28 — tournament context on Bangladesh login

Only UniversalLoginActivity opened through the existing tournament-promotion
launcher displays the following introduction above its unchanged provider buttons:

| | English | Bengali |
|---|---|---|
| Heading | Join the tournament | টুর্নামেন্টে যোগ দিন |
| Explanation | Sign in or start playing to continue to the selected tournament. | নির্বাচিত টুর্নামেন্টে এগিয়ে যেতে লগইন করুন অথবা খেলা শুরু করুন। |

The two resources are translated into all 20 supported app languages. The view
is included only by the Bangladesh layout and defaults to GONE. A normal login
has no introduction, including when stale pending selection data exists. Global
does not include or show the view and retains its original login event behavior.

## Single authoritative selection

PendingTournamentPromotion still owns the selected public tournament ID, owner
and timestamp in its existing preference keys. No second selection or persistent
source state is introduced. Its loginIntent factory adds only a boolean launch
marker, not a tournament ID. isLoginEntry requires both this marker and a valid,
unclaimed selection within the existing 24-hour age limit. Checking presentation
does not bind, consume, clear or rewrite the selection. The Intent marker survives
Activity recreation; it is not sufficient by itself to imply a tournament flow.

Normal login intents have no marker. Missing, expired, future-dated, invalid or
already-owned selections do not produce tournament login wording. No network
request is needed to decide whether to show the introduction.

Successful authentication still only finishes the login Activity. Menu keeps its
existing profile, nickname and application Terms gates, revalidates the selected
tournament, and resumes the existing RegulationActivity/lobby route. It clears
the pending selection at the same points as before. Cancelling login without an
authenticated user returns to Menu and the original Activity-result logic clears
the selection. This handler was extracted into a named method without changing
its conditions or effects. No authentication/provider behavior, eligibility,
RegulationActivity, joining request or payout implementation changed.

## Analytics

Bangladesh reuses **onboarding_step**, with:

- stage = authentication
- outcome = shown
- reason = none
- source = tournament or account

The source is derived from the same presentation check. Arbitrary source values
fall back to account. There is no method parameter before a method is selected,
no user-entered value, and no new event name. Automatic screen_view and successful
authentication login/sign_up events retain their meaning. Recreating login may
produce another screen-open milestone, consistent with existing screen visits;
these are not unique account counts. Global still sends its legacy empty login
event on screen opening, with no added source parameter. The no-argument tracking
method remains available and defaults to account.

No GA4 custom dimensions or backend configuration were created. The new source
parameter can be inspected in DebugView and BigQuery; a custom event dimension
would be a separate configuration decision if needed in standard GA4 UI reports.

Provider/anonymous login progress in both BD and Global now uses the shared game/menu loading
overlay with the existing localized login_in_progress message. The overlay
follows the existing button enabled state and is hidden by success/failure
callbacks (including provider cancellation). Both variants also use that panel
for nickname checking and Terms acceptance saving, with their existing localized
messages. Global authentication/Terms navigation semantics remain unchanged;
tournament-specific login and Terms wording remains BD-only.
The Terms privacy link also uses the same underlined link presentation as
Account, and BD Decline uses sentence case like Accept & continue.

## Focused validation

TournamentLoginContextTest covers actual login rendering, normal login with a
pending selection, missing/expired/future/owned selections, recreation, Bengali
wording, anonymous success retaining the selection, Google/Email destinations,
and cancellation returning to Menu. MenuTournamentPromotionTest also follows a
guest card selection through incomplete Terms/nickname gates to the existing
selected-tournament continuation. AnalyticsFunnelTest checks the controlled
source and unchanged Global event payload. Existing anonymous warning/provider,
authentication diagnostics, pending login, Facebook helper and Terms suites are
included in both DEV variant verification.

Verification result: 181 tests passed, 9 flavour-specific cases skipped, zero
failures (95 selected tests in each variant). assemble_devBangladeshDebug and
assemble_devGlobalDebug both succeeded. All 20 localization XML files parsed
with two nonempty resources. Physical phone verification is recorded below.
The email profile-fallback regression fixture was updated to advance its test
clock through the already-existing profile-read retries; production email
authentication code was not modified.

## Physical phone smoke test — 2026-11-18

Samsung SM-A505FN, installed 17.28-BD (versionCode 63), DEV/debug. The
operator authorized ADB interaction with a disposable DEV account. No production
data or application code was changed during this test.

- 12:13:50: promotion click opened login with the tournament heading and body;
  onboarding_step had stage=authentication, outcome=shown, source=tournament.
- Start Playing / Proceed completed anonymous authentication. Application Terms
  showed the tournament heading and entry_source=tournament.
- 12:14:46–48: accepting Terms visibly showed the saving spinner/message and
  disabled both buttons. terms_save submit and success were recorded, about
  1.77 seconds apart. Nickname setup appeared after successful saving.
- Suggested nickname confirmation completed. login_success followed nickname
  success, and the original selected tournament resumed in RegulationActivity.
- Two checked confirmations left Accept disabled; all three enabled it.
  Regulation load, confirmations_visible and accept_ready were recorded.
- 12:16:23–28: tournament_join_submit then tournament_join_success for the same
  tournament. Menu no longer displayed its card; the tournament list showed
  JOIN disabled and LEAVE available. Closing an intervening test ad correctly
  continued to the tournament list.
- After logging out of the disposable account, normal Account login displayed
  no tournament heading/body. At 12:18:28 the authentication shown event had
  source=account. Back returned safely to the logged-out Menu.

Firebase SDK logs confirmed app_environment=_dev and app_build_type=debug, with
successful HTTP 204 uploads. Firebase DEV DebugView (GA4 property 503027670)
also displayed the matching 12:13–12:18 timeline, including promotion click,
authentication events, Terms/nickname milestones, login_success, regulation
milestones, tournament_join_submit and tournament_join_success. The two source
parameter values were verified in SDK event payloads, not the DebugView details
panel. Evidence screenshots and SDK logs are retained locally in ignored
mobile/build/17.28-test-*.png and mobile/build/17.28-phone-analytics.log.

This smoke test did not inject a save failure, hold a request to test Back during
saving, or retest Global on the phone. Those cases retain the focused automated
coverage described above. The phone was left at the logged-out DEV Menu.
