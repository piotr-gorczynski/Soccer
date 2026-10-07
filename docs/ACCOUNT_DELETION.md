# Account deletion: preserve historical integrity

Implementation prepared 2026-10-07; not deployed. This applies to the shared account used by both variants. The website privacy revision is an unpublished draft, not a change to the live policy.

## Data boundaries

`removeAccount` deletes Firebase Authentication, then replaces `users/{uid}` (without merge) with only:

- `accountDeleted: true`
- `nickname: "(Account removed)"`
- `nicknameLowercase: "(account removed)"`
- `accountDeletedAt`: server timestamp (kept stable on retries)

Replacement strips unknown/future profile fields as well as email, provider IDs/names/photos, FCM token/installation ID, tracking metadata and deprecated profile-level acceptance flags. It does **not** delete the root document, legalAcceptances or any other subcollection by recursion.

The same UID remains in matches, moves, tournament participants/results, rankings, payout/support and other historical records. No UID migration, history rewrite or blanket UID search-and-delete is performed. Such data is pseudonymous, not necessarily anonymous. Historical data can still contain previously captured personal information; this is not claimed to be complete anonymization or historical PII redaction.

If an unversioned legacy acceptance flag exists, its available timestamp/language are archived as `legalAcceptances/legacy_terms_unversioned` with unknown scope and null version, before the profile is replaced. No historical document version, URL or hash is invented. When a modern acceptance has overwritten the old timestamp, the archived legacy timestamp stays null. This archive is server-only and is never a valid current Terms acceptance.

New versioned Terms evidence remains under `users/{uid}/legalAcceptances`. Tournament eligibility/regulation acceptance remains in participant records. Payout retention is unchanged: 180 days raw and five calendar years minimized audit after completed/cancelled, subject to existing holds; account removal never resets deadlines. There is no approved numeric Terms/regulation retention duration identifiable in the repository, so no period is invented and no automatic deletion of that evidence is added. Confirm the legal retention policy before publishing claims of a specific period.

## Active state and retries

- `status/{uid}` in Realtime Database becomes only `{accountDeleted:true,state:"offline"}`. This technical marker blocks stale-token and onDisconnect writes via RTDB rules; no heartbeat, device data or active presence is retained there.
- Pending invitations sent or received by the account are deleted, transactionally checking they are still pending. Accepted/expired/historical invitations are not purged.
- Own `users/{uid}/friends/*` and incoming `users/{other}/friends/{uid}` are deleted in bounded batches. This changes active relationships only. Incoming relationships require paging user IDs because existing friend documents encode the target solely as document ID.
- `onAccountDeleted` is a retry-enabled Firebase Auth deletion trigger in the same remove-account codebase. It completes cleanup after an Auth deletion even if the callable fails or the user can no longer authenticate. It handles console and scheduled Auth deletions too. The callable also runs cleanup and treats auth/user-not-found as a retry, not a blocker. Duplicate runs are safe; errors are surfaced rather than claiming completion.
- Existing inactive/unverified cleanup functions no longer delete or overwrite `users/{uid}`; they delegate its minimization to the new Auth handler so existing legal evidence is read before replacement. They require that handler for profile minimization and complete active-state cleanup. No scheduled eligibility criteria were changed.
- Firestore rules block client edits to tombstones and creation of deleted markers; friends cannot be created for a deleted endpoint. RTDB rules block reactivation. Active friend/invitation/tournament callables and variant tracking check deletion inside their transactions. Notification error handlers do not recreate fields on a concurrently deleted profile.

No existing cloud records are changed by this implementation task. Bulk Auth `deleteUsers` does not emit individual onDelete events; do not use it as an account-removal mechanism without explicitly running equivalent cleanup. Automatic retry delivery has platform limits; monitor failed Auth deletion executions and retry the idempotent cleanup for the same UID if exhausted. Do not restore profile information to repair a partial failure.

## Display

`UserDisplayName` prioritizes `accountDeleted` over any retained/stale profile nickname, never falls back to email/provider identity, and uses the removed-account name for a missing profile. History/standings screens resolve names from profiles; ranking/results and cached friend/match/invitation rows listen for profile changes and release listeners on destruction/detachment. Historical UID references are not rewritten. Offline copies cannot learn a deletion until synchronization resumes.

## Deployment prerequisites (not executed)

Deploy remove-account codebase including **both** `removeAccount` and `onAccountDeleted`; its existing Cloud Build YAML now copies the helper. Deploy changed cleanup-inactive-users, delete-unverified-users, add-friend, create-invite, accept-invite, join-tournament, track-app-variant, update-payment-status and support-tickets codebases, plus Firestore and RTDB rules. Release the Android display changes after testing; separately approve/publish the privacy revision. No IAM, cloud data, deployment, publication, commit or push is part of this task.

## Validation

`npm run test:account-deletion` in tools/test-payout-rules uses only local demo Firestore/RTDB emulators. It checks minimal replacement, preserved game/UID/legal/payment data, both-direction friends, pending-vs-historical invitations, retry/idempotency, missing profiles and stale-token security rules. Run the existing payout/retention/Terms suite as a regression check. Android UserDisplayName tests check that a deleted marker wins over a former nickname.

Local verification (2026-10-07): Auth/callable orchestration 4/4; Firestore/RTDB deletion integration 5/5 (including more than 300 incoming friend relationships); existing payout/retention/Terms emulator tests 19/19. Android Global and Bangladesh debug builds passed, with 19 focused tests per flavor. No real account deletion was executed; on-device multi-account/offline verification remains a release check.
