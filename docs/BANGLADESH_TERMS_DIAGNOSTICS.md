# Bangladesh Terms UX and diagnostics

Only editable Bangladesh Terms screens display the introduction and the new CTA.
Global and read-only legal screens retain their existing presentation. Legal URLs,
verified document hashes/versions, acceptance persistence and sign-out on refusal
are unchanged.

English copy:

- Heading: **One more step to finish setting up your account**.
- From a tournament: **One more step to join the tournament**.
- Explanation: **Please read and accept the application Terms and Privacy Policy to continue.**
- CTA: **Accept & continue**.

Bengali copy:

- Heading: **আপনার অ্যাকাউন্টের সেটআপ শেষ করতে আর একটি ধাপ**
- From a tournament: **টুর্নামেন্টে যোগ দিতে আর একটি ধাপ**
- Explanation: **এগিয়ে যেতে অ্যাপের শর্তাবলি ও গোপনীয়তা নীতি পড়ে সম্মতি দিন।**
- CTA: **সম্মতি দিয়ে এগিয়ে যান**

The four resources are translated into all 20 supported app languages. They are
not shown in Global. Tournament context comes from a valid pending promotion,
an explicit join/regulation destination in the Menu Terms gate. Merely browsing
the tournament list, results, lobby or prize notifications does not imply a join
flow and uses neutral account
wording. Unknown context defaults to normal onboarding. The explanation always
identifies these as application Terms, independent of tournament context.

## Analytics contract

Reuse `onboarding_step`; no second acceptance/error event or new GA4 definitions.
Bangladesh non-read-only screens send these stages/outcomes:

| stage | outcome | meaning |
|---|---|---|
| terms_screen | shown | Activity first resumed; once across saved-state recreation |
| terms_screen | paused | Foreground interval ended; **not** a refusal |
| terms_load | success / failure | Verified document fetched / fetch failed |
| terms_save | submit | Existing acceptance write started |
| terms_save | success | Existing acceptance write succeeded for the same user |
| terms_save | failure | Acceptance write failed; explicit retry remains available |
| terms_save | declined | Explicit Decline or Back; `action` distinguishes them |

Every event contains only `stage`, `outcome`, `reason`, `code`, `entry_source`
(`tournament` / `onboarding`), `action` (`none` / `accept` / `decline` / `back`),
and `duration_ms`. Duration is cumulative foreground elapsed time, excluding
background intervals and capped at 24 hours. Successful acceptance or refusal
closes tracking to prevent duplicate terminal events. Process termination cannot
reliably send a final event: a pause is not proof of abandonment. A load success
does not mean acceptance. Existing Global event behavior is unchanged.

## Controlled failure classification

Firestore SDK codes map to permission, unauthenticated, unavailable, timeout,
resource_exhausted, failed_precondition, not_found, already_exists, aborted,
cancelled, invalid_argument, out_of_range, internal, data_loss, unimplemented or
unknown. `code` retains only the SDK enum name. `UNAVAILABLE` is not assumed to
prove a connectivity failure. Socket timeout maps to timeout/TIMEOUT;
FirebaseNetworkException, UnknownHostException and ConnectException map to
network/NETWORK_ERROR; other IOException maps to io_error/IO_ERROR; unrecognized
exceptions map to unknown/UNKNOWN. Causes are inspected with a bounded traversal.
Exception messages are never parsed or sent; arbitrary class names, stack traces,
UIDs and user-entered text are not included in these diagnostics.

The recent historical `unknown` cannot be retroactively reclassified from its
aggregate GA4 record. Existing Terms code collapsed Firestore failures into that
value; these changes improve future observations, not the historical evidence.

## Validation

Focused suites: TermsActivityTest, TermsDiagnosticsTest, TermsPolicyTest,
TermsRepositoryTest, TermsEntryActivityTest, MenuTermsReadFailureTest,
AnalyticsFunnelTest and MenuTournamentPromotionTest in both DEV market variants.
Build targets: assemble_devBangladeshDebug and assemble_devGlobalDebug.
All 20 translation XML files are parsed and checked for four nonempty resources.
No backend deployment or Firebase/GA4 configuration change is required.

### Manual DEV phone verification — 15 November 2026

Bangladesh 17.27-BD, DEV project soccer-dev-1744877837, GA4 property 503027670.
ADB confirmed event generation and successful uploads (HTTP 204). Firebase
DebugView subsequently showed the matching timeline, including onboarding_step,
login_success, tournament_regulation_step and tournament_join_submit/success.
The Debug Device selector displayed 0/undefined despite a populated timeline;
that selector alone must not be interpreted as missing event delivery.

- Tournament card impression and click, anonymous warning shown/proceed,
  login_attempt and successful login/sign_up were observed.
- Tournament-context Terms displayed the tournament heading and reported
  entry_source=tournament for shown, load success and acceptance submit/success.
- Separate runs reported action=back (56504 ms) and action=decline (1551 ms).
- Nickname submit/success preceded login_success. No nickname value was included
  in those event payloads.
- Regulation load, confirmations_visible and accept_ready were observed.
  Two checked confirmations kept Accept disabled; three enabled it and reported
  checked_count=3. The DEV join submitted at 15:59:14 and succeeded at 15:59:15.
- Normal onboarding displayed the neutral account heading and reported
  entry_source=onboarding. Home produced paused, not declined, and returning did
  not repeat shown. Final acceptance reported duration_ms=731875, excluding the
  approximately 9.2-second background interval.

This verifies the exercised success/refusal/background paths. It does not claim
manual device verification of Terms load/write failures, all SDK error mappings,
or arrival in delayed BigQuery exports; controlled error mapping is covered by
the automated tests.

## 17.28 — protect an in-flight acceptance write (Bangladesh only)

After explicit acceptance, show the shared game/menu loading overlay (dimmed
background, centered loading panel and indeterminate progress indicator) with:

- EN: **Saving your acceptance. Please wait.**
- BN: **আপনার সম্মতি সংরক্ষণ করা হচ্ছে। অনুগ্রহ করে অপেক্ষা করুন।**
- PL: **Zapisywanie Twojej zgody. Proszę czekać.**

This resource is translated into all 20 supported application languages.
Accept and Decline are disabled while the existing write is pending. Back stays
on the screen and repeats the saving message, without signing out or emitting a
false declined event. A failure hides progress and restores retry/refusal;
success advances only after server acknowledgment for the same account/policy.
No timeout pretends that an unfinished write was cancelled or successful.

A screen-scoped ViewModel retains the single request across recreation. It owns
no Activity; the current screen observes results while active. Background
completion waits for return. A result for a different account cannot acknowledge
the new account. Existing submit/success/failure events and controlled error
mapping are reused; no additional analytics event or user data is added.

Legal content, consent requirements, repository transaction, RegulationActivity,
and Global UI/navigation behavior are unchanged. The shared Gradle version moves
to 17.28 (code 63), producing 17.28-BD for Bangladesh. This change needs an Android
release, not a backend deployment. The 17.28 phone smoke-test results are in
BANGLADESH_TOURNAMENT_LOGIN_CONTEXT.md. Those screenshots precede this visual
overlay adjustment; the write behavior is unchanged.
