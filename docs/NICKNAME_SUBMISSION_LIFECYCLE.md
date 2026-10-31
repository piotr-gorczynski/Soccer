# Nickname submission lifecycle recovery

Prepared for 17.23 (versionCode 58), 31 October 2026, in the shared Global/Bangladesh nickname screen.
No backend deployment or authentication/Terms changes are required.

The old uniqueness and moderation callbacks used Activity-scoped Firebase Task
listeners. Google Tasks removes these listeners at `Activity.onStop()`. A delayed
successful moderation response could therefore never reach the Firestore write.
The 30 October production trace is consistent with this failure: moderation
returned allowed/HTTP 200, but no nickname write or client outcome followed.
The function logs do not identify the caller, so temporal correlation is not proof
of the precise device lifecycle transition.

`NicknameSubmission` now owns uniqueness, moderation and the existing merged
Firestore write in an Activity-scoped ViewModel. SDK callbacks are not tied to an
Activity. Backgrounding and configuration recreation preserve the pending work;
the paused screen detaches its observer, and the resumed screen receives the
pending state or terminal result. Success/failure analytics are emitted at the
operation outcome, once, without entered text or exception messages. Existing
validation rules, event names and controlled failure categories remain unchanged.

Before each asynchronous continuation, the account must still match the submitting
UID. A changed session stops later stages and cannot populate a new session's
nickname cache or navigate it on behalf of the old account. An already submitted
server write cannot be recalled, but its callback cannot update another session.
Finishing the screen clears its ViewModel and prevents further follow-on work.

This is in-process recovery, not a durable background job. Killing the process can
still interrupt a not-yet-submitted write. On restart the existing profile read
determines whether nickname setup is still needed; no automatic resubmission or
new retry policy is introduced.

## Device regression

Use a disposable DEV account and a rebuilt DEV Bangladesh or Global APK:

1. Reach nickname setup, enter a valid unused nickname and confirm once.
2. Immediately press Home while moderation is pending; wait for the response.
3. Return to the app. Expect the saved nickname/menu, or a visible existing error
   permitting retry. The screen must not remain stuck checking indefinitely
   merely because it was backgrounded.
4. Repeat with screen recreation while the request is pending (if rotation is
   enabled). Expect one operation, no duplicate write, and delivery to the new
   screen rather than the destroyed one.
5. Verify a taken nickname and a moderation rejection still block saving; verify
   uniqueness, moderation and write failures show the existing messages.
6. Verify `nickname` submit/success or the applicable failure event and the actual
   Firestore nickname. Complete the existing Terms flow as required before
   expecting `login_success`.

Focused automated suites: `NicknameSubmissionTest`, `NicknameEntryUxTest` for
both DEV markets. The delayed Task tests cover stopped-screen completion,
replacement observers, duplicate submission prevention, account changes,
ViewModel clearing, all operation failure stages and safe Analytics payloads.

References:
- [Google Tasks Activity-scoped listeners](https://developers.google.com/android/guides/tasks)
- [Android ViewModel and configuration changes](https://developer.android.com/topic/libraries/architecture/viewmodel)

Verification (31 October 2026): 23/23 focused tests passed for DEV Bangladesh and
23/23 for DEV Global; both DEV Debug APK builds succeeded. These results do not certify the full test suite.

Owner-reported DEV Bangladesh phone regression passed on 31 October: paused at
the breakpoint before moderation, pressed Home, resumed execution, waited and
returned to the app. The menu showed the new nickname. After closing and reopening
the app, the same nickname remained and nickname setup did not reappear. This
verifies that device scenario; it does not establish the exact cause of the
historical production case or certify process death while a write is pending.
