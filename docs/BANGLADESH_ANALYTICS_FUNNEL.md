# Bangladesh acquisition and onboarding instrumentation

Audit and source changes: 23 October 2026. Prepared for 17.19 (54), not a claim of
store rollout, production collection, Ads import or GA4 configuration changes.

## Findings before implementation

All app `logEvent` calls are centralized in `AnalyticsManager`; Bangladesh uses
the common Java source with market BuildConfig `bangladesh`. Build defaults add
`app_variant`, `app_environment`, `app_build_type`; SDK provides app version and
screen class. No new user identifier is needed for the funnel.

The production config maps `piotr_gorczynski.soccer2.bd` to Firebase app
`1:249008569891:android:76386af0099ba9ba91bb96` in `soccer-prod-1754048346`.
The inspected GA property is `503154947`, BD stream `15866019828`. DEV/TEST use
their own projects, even though the installed Android package has the same name.

### Existing emitted events and coverage

| Event | Existing trigger / limitation |
|---|---|
| `first_open` | Automatic SDK event, not a Google Play download event; no app implementation required. See verification below. |
| `screen_view`, `session_start`, `user_engagement` | Automatic lifecycle collection. Screens cover login, nickname, Terms, tournament list, regulation/details and lobby. No duplicate custom view events needed. |
| `app_update`, `app_remove`, `os_update`, `app_clear_data` | Automatic when applicable; uninstall events are not a measured individual onboarding sequence. |
| `ad_impression`, `ad_click` | Mobile Ads integration, when applicable; these concern ads displayed INSIDE the game, not clicks acquiring this app. |
| `app_exception` | Crashlytics integration when applicable; separate from auth outcomes. |
| `login` | Legacy code emitted this on UniversalLoginActivity creation, not successful authentication. Not a valid historical auth-success denominator. |
| `sign_up` | Legacy UniversalLogin callback, including returning OAuth/anonymous logins; email registration did not consistently emit it. Not historical new-account counts. |
| `login_success` | 17.18: successful explicit sign-in/profile callback BEFORE nickname/Terms. Already-prepared 17.19: pending explicit login consumed after server-confirmed required Terms and nonblank server nickname. Existing complete accounts also emit on explicit login; restoration alone does not. |
| `sign_up_error`, `auth_cancelled` | Existing central auth diagnostics with provider, operation step and SDK code. Covers profile/verification/reset failures as well as sign-in; do not treat every error as a failed login. Cancellation is Analytics-only; other auth failures also record safe non-fatals. |
| `signup_decline_reason` | Controlled choices in the registration prompt; reasons are now allowlisted defensively. |
| `nickname_check_error` | Previously only AI-check failures, with nickname and free-text message in Analytics/breadcrumb. Unsafe payload removed; compatibility name retained. |
| `tournament_view` | Snapshot-loaded list counts; previously emitted repeatedly on listener changes. Now one per BD Activity instance, not every snapshot. |
| `tournament_join_start` | Join button selected BEFORE authentication prompt/regulation; not the final backend submission. |
| `tournament_join_success` | Successful joinTournament callable response; backend commits participant before returning `{ok:true}` and rejects already-joined accounts. |
| `tournament_join_error` | Existing callable failure. Previously sent raw error text; now bounded reason only, also covers token-refresh failure. |
| `prizes_view`, `prize_details_view` | Existing prize list/detail opening events; retain. |
| `app_variant_conflict_shown`, `app_variant_conflict_closed` | Existing installed-variant conflict flow; retain as a possible earlier access barrier. |
| `tutorial_completed` | Existing custom event for hand tutorial/messages, not recommended `tutorial_complete`; no second tutorial event added. |
| `games_played_before_signup` | Existing anonymous-to-provider upgrade metric, not a new installation or account counter. |
| `first_game_win` | First game COMPLETION with `won` parameter, including losses. Do not interpret event count as wins. |
| `time_to_first_game` | First game start relative to locally saved first-open timing, not a substitute for first_open or Ads attribution. |

Other methods beginning `add...Breadcrumb` write Crashlytics diagnostics, not GA4
events. `AnalyticsTestActivity` can deliberately emit synthetic events; do not
use its output as acquisition evidence. SDK-internal/debug/consent events may also
appear; this inventory does not claim every optional SDK event is always emitted.

## Chosen small funnel (BD schema 2)

| Stage | Signal and exact trigger |
|---|---|
| Download | Google Play / Ads installation action; outside this client funnel. |
| First use | Automatic `first_open`; never synthesize or log it manually. |
| Authentication screen | Automatic `screen_view`, `firebase_screen_class=UniversalLoginActivity` or `LoginActivity`. |
| Auth request | New `login_attempt` with normalized `method`; OAuth before provider UI, email after nonempty form submit, anonymous after confirming warning. Not every screen open or keystroke. |
| Auth succeeded | Recommended `login(method)` at successful Firebase credential result, before profile operations. Email requires verified email. No event on session restoration. |
| New account | Recommended `sign_up(method)` only with Auth `AdditionalUserInfo.isNewUser()` evidence, or successful email createUser result. New OAuth/anonymous accounts can legitimately emit BOTH sign_up and login; these are different milestones, not additive user counts. |
| Nickname reached | Automatic `screen_view=PickNicknameActivity`. |
| Nickname outcome | New `onboarding_step`: `stage=nickname`, `outcome=submit/success/failure`. Success only in server-write success callback. Uniqueness/content/profile-write failures use their own controlled stages/reasons; AI backend failure reuses `nickname_check_error` instead of a duplicate failure event. |
| Terms reached | Automatic `screen_view=TermsActivity`. |
| Terms outcome | Same `onboarding_step`: `stage=terms_load` (verified HTML fetched / failure), `stage=terms_save` (submit / successful transaction / failure / explicit decline). Read-only Terms viewing emits none of these outcome events. A load success does not prove the user read the document. |
| Ready after login | Preserve prepared 17.19 `login_success(method)` after both server checks. Do not add duplicate onboarding_complete/profile_setup_complete events. It counts explicit completed sign-ins, not unique newly onboarded accounts. |
| Tournament list | Existing `tournament_view`, automatic `screen_view=TournamentsActivity`. |
| Details/regulation | Automatic `screen_view=RegulationActivity` (join flow), `TournamentLobbyActivity` or `TournamentResultsActivity` as applicable. There is no separate universal tournament-details screen to instrument twice. |
| Join intent | Existing `tournament_join_start` (public tournament ID, numeric authentication flag). |
| Confirmed join request | New `tournament_join_submit` after local eligibility/auth checks, before token refresh/call. Distinguishes regulation abandonment from a submitted attempt. |
| Join result | Existing `tournament_join_success` / sanitized `tournament_join_error`. |

Only THREE new custom event names: `login_attempt`, `onboarding_step`, and
`tournament_join_submit`. No duplicate install, view, login_failure, terms_accept,
nickname_setup_complete or onboarding_complete events. Existing `sign_up_error`
and `auth_cancelled` remain the auth failure/cancellation signals.

### Continuity and scope

New granular signals and corrected recommended auth events apply to Bangladesh.
Global retains its legacy `login`/`sign_up` meanings. Shared legacy error helpers
are sanitized for both markets; Boolean GA parameters are converted to numeric
0/1 because GA event parameters use String/long/double types.

Default `funnel_schema=2` identifies new BD instrumentation (Global uses 1).
Do not combine historical `login` screen opens with schema-2 `login` successes.
Do not combine 17.18 login_success with 17.19 readiness counts without splitting
by app version. Neither rename nor erase historic data. Existing Ads imports are
unchanged; review the version mix before importing login_success as an objective.

### Privacy and volume

No nickname, email, name, UID, phone, wallet, free-text refusal, raw SDK message,
URL with account data or arbitrary form value is added to new events. Local
pending-attempt UID/token used by the prepared 17.19 gate is NOT transmitted.
`method`, `stage`, `outcome`, and `reason` have fixed vocabularies; unknown caller
values map to `unknown`. Existing tournament IDs identify public tournaments,
not players; no new tournament titles or participant IDs are added.

Auth failure `reason` is bounded to cancelled/network/invalid_credentials/
provider_error/unknown; detailed existing SDK `code` and `step` remain for
diagnosis. Missing metadata is not guessed from exception text. Technical nickname
and Terms failures are not automatically user cancellations. Failure to return
is inferred from the funnel; do not emit an unreliable app-exit/dropout event.

One event per explicit action or result, not per keystroke, checkbox, frame,
timer or listener snapshot. Activity recreation can produce another screen view
and list view; these are visits, not unique users. Existing backend effects and
consent choices are unchanged.

## first_open verification: not yet proven end-to-end

On 23 October, a live report filtered to BD stream `15866019828`, dates 19–23
October, event `first_open`, showed no data. Correct package/project config and
other BD events were confirmed, but cannot establish first_open collection.
The app defaults analytics storage to denied; ConsentUtils updates it after UMP
resolution. Consent, debug-device exclusion, report latency, and reinstall/test
history must be considered. No consent bypass or duplicate event was introduced.

Before release, use a disposable DEV installation (never erase a real PROD
anonymous account): verify generated google_app_id against the DEV BD Firebase
app, enable Analytics DebugView for the BD package, launch a genuinely fresh app,
complete the normal consent flow, and inspect automatic first_open and any
firebase_error. Then repeat a non-debug clean-install smoke test with consent on
the release track and allow standard report processing. Reopening/updating an
existing installation is not a valid first_open test. Record results separately
for denied/allowed consent. If still missing, investigate SDK/consent delivery
before treating Ads installations and first_open as a measured cohort.

## Reporting setup and acceptance

Create event-scoped custom dimensions only if absent: funnel_schema, method,
stage, outcome, reason (and existing code/step only when needed). App version,
country and stream are built-in. Do not register player IDs, free text or attempt
tokens. Custom dimension reporting is not retroactive; DebugView can inspect raw
parameters before dimensions are created.

Use GA4 Exploration with BD stream, production environment and schema 2. Segment
new users and acquisition source/campaign separately from country; Bangladesh
geography alone is NOT proof of Ads acquisition. Exclude known QA traffic with
appropriate test/debug segments rather than creating production test accounts.
Do not sum Ads downloads and Analytics first_open or infer conversions by
subtracting unrelated date-window totals. Standard reports are delayed and
consent can prevent an identifiable ordered journey.

Primary ordered path: first_open -> login_attempt -> login -> nickname screen ->
nickname success -> Terms screen -> Terms success -> login_success ->
tournament_view -> tournament_join_start -> regulation screen ->
tournament_join_submit -> tournament_join_success. Use an open funnel/optional
branches as well: returning accounts skip nickname/Terms, Google may supply an
existing nickname, and Terms/nickname order can differ by entry path. Compare
user-based step completion, elapsed time and failure reasons, not raw event sums.

Manual DEV checks: new anonymous account pauses before nickname (no login_success),
empty/taken/rejected nickname, network/backend failure, nickname save success,
Terms load failure/retry, decline, failed acceptance/retry then success, existing
Google login, provider cancellation, failed email login, restart after completion,
regulation abandonment and confirmed join success/failure. Verify read-only Terms
and listener updates do not inflate completion events, and no sensitive values
appear in DebugView. Do not deliberately join a cash-prize PROD event for testing.

## DEV device verification on 23 October 2026

Manual testing verified anonymous authentication, empty nickname validation,
nickname persistence, Terms loading/acceptance, one post-onboarding login_success,
no duplicate authentication events on restart, tournament listing and successful
join, Facebook cancellation, and offline authentication failure. The offline test
exposed FirebaseNetworkException being classified as provider_error; this was
corrected to network, regression-tested and verified again on the phone.

GA4 property 503027670, Bangladesh stream 13219161884, maps to Firebase app
1:491861032786:android:32ac2a4516b01b275f048b. Realtime and subsequently DebugView
both displayed the tested funnel events; DebugView exposed reason=network for
the corrected failure. Its device selector still displayed 0/undefined despite
populated event timelines, so that counter alone is not a reliable receipt check.
The initial empty DebugView was transient; its exact cause was not established.

Fresh installation emitted automatic first_open at 11:48:12 in device logs and
uploaded successfully (HTTP 204), initially before analytics_storage was granted
at 11:48:20. first_open was not confirmed in DebugView: pre-consent emission is
distinct from visibility in user-level reports. No manual first_open was added,
and no consent restrictions were bypassed.

## Official references checked on 23 October 2026

- [Firebase Android event logging and custom parameter reporting](https://firebase.google.com/docs/analytics/android/events)
- [Recommended login and sign_up semantics](https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Event)
- [Automatically collected events, including first_open and screen_view](https://support.google.com/analytics/answer/9234069)
- [Event naming and reserved names](https://support.google.com/analytics/answer/13316687)
- [Collection limits: 40-character names, 25 parameters, 100-character string values](https://support.google.com/analytics/answer/9267744)
- [Avoiding PII in event parameters](https://support.google.com/analytics/answer/6366371)

New names are lowercase letters/underscores, under 40 characters, and do not use
reserved prefixes. New events have at most five short scalar parameters plus
four default metadata parameters, below GA4 limits. No custom event uses a
reserved automatic name. No Google Ads goal, bidding, budget, or Analytics admin
configuration was changed by this implementation.
