# Player moderation

Implementation prepared locally; not deployed by this change.

## Player actions

The player menu (three dots) is available in friends, player search, pending invitations,
rankings and tournament results. Signed-in players can report another player/nickname
with a reason, block that player, or unblock them. Players can unblock through the per-player menu, including player search. The Friends toolbar has no separate blocked-players menu.

All 11 new moderation strings are localized in all 20 supported application languages.

Blocking is independent of `blockInviteFriend`. A block in either direction prevents
new invitations (including tournament invitations), accepting a previously sent
invitation, and adding the other player as a friend. It does not automatically remove
an existing friendship, ban anyone, change nicknames, cancel an already active match,
change tournament results or award walkovers. Existing invitations remain in history;
acceptance is denied while blocked. Queued notification handlers recheck blocks before
sending, but an already delivered push notification cannot be recalled.

## Backend and privacy

`reportPlayer` and `setPlayerBlock` are callable functions in the support-tickets codebase.
Both require an authenticated, active profile. The server derives the reporter/owner
UID from authentication. It obtains the reported nickname from the current profile
and assigns server timestamps. Caller-supplied identity, nickname or timestamp fields
are rejected. The screen context is allowlisted and explicitly marked client-reported;
it is not evidence that misconduct occurred. Reasons are limited to 1,000 characters.
No automated action is taken against a reported player.

- `users/{uid}/blocks/{otherUid}`: createdAt only; owner can read, callable-only writes.
- `users/{uid}/moderationState/reports`: server-only submission serialization metadata.
- `supportTickets/{id}`: type moderation, category player_report, userId/reporterUid,
  reportedUid, nickname snapshot, reason/message, appContext, status and timestamps.
- `supportTickets/{id}/statusHistory/created`: server-written report creation audit.

Only the reporter and authorized admins can read the report. One unresolved report per
reporter/target is accepted; repeated submissions return its reference. A reporter can
have at most ten open moderation reports. A fresh report after resolution preserves the
previous report and its history. This is basic queue protection, not a comprehensive
anti-abuse system. Reports and blocks are personal data; their approved retention is described below and is independent of payout retention.

## Admin handling

Use existing `tools/support-tickets/support-tickets.js <env> list --status open`.
The table includes reported UID, reporter UID and nickname. Existing `reply` and `resolve`
commands preserve support history. Moderation reports deliberately do not emit the
payout-specific push/deep link. The reporting UI confirms the report reference; it does
not promise a reply inbox. Administrators review reports manually; no automatic bans or
nickname changes are added.

## Release and validation

No deployment performed. Deploy together using existing mechanisms: support-tickets
(codebase, including the two new callables and notification handler), Firestore rules,
createInvite, acceptInvite, addFriend and sendInviteNotification. The support build YAML
now copies moderation.js into its deployment workspace. Release the Android build only
after these backend changes. Existing deployed builds do not have the new UI.

Emulator suite: `npm --prefix tools/test-payout-rules test` includes moderation tests,
identity forgery/privacy rules, bidirectional block enforcement, old invitations,
tournament invitations, unblocking, idempotency, concurrency, queue limits and queued
notification suppression. Android builds: `_devGlobalDebug` and `_devBangladeshDebug`.
On-device interaction/accessibility checks remain a release check; no live data is used
by emulator tests. This feature alone is not a certification of all Google Play UGC
policy requirements (including applicable Terms and moderation operations).

## Verification results (9 October 2026)

- Firestore emulator suite: 24/24 passed (5 moderation scenarios plus 19 regression tests).
- Existing support-ticket unit tests: 2/2 passed.
- Both `_devGlobalDebug` and `_devBangladeshDebug` assembled successfully.
- Gradle reports existing deprecated features incompatible with Gradle 10.
- No deployment, live-data mutation, commit or push.

## Files changed

- `docs/PLAYER_MODERATION.md`
- `firebase/firestore.rules`
- `firebase/functions/accept-invite/index.js`
- `firebase/functions/add-friend/index.js`
- `firebase/functions/create-invite/index.js`
- `firebase/functions/send-invite-notification/sendInviteNotification.js`
- `firebase/functions/support-tickets/index.js`
- `firebase/functions/support-tickets/moderation.js`
- `gcp/cloud-build/deploy_support_tickets.yaml`
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/FriendAdapter.java`
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/FriendsListActivity.java`
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/PendingInviteAdapter.java`
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/PlayerModeration.java`
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/RankingActivity.java`
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/TournamentResultsActivity.java`
- `mobile/app/src/main/java/piotr_gorczynski/soccer2/UserSearchAdapter.java`
- `mobile/app/src/main/res/layout/item_friend.xml`
- `mobile/app/src/main/res/layout/item_pending_invite.xml`
- `mobile/app/src/main/res/layout/item_standing.xml`
- `mobile/app/src/main/res/layout/item_user_search.xml`
- `mobile/app/src/main/res/values-am/strings.xml`
- `mobile/app/src/main/res/values-ar/strings.xml`
- `mobile/app/src/main/res/values-de/strings.xml`
- `mobile/app/src/main/res/values-es/strings.xml`
- `mobile/app/src/main/res/values-fa/strings.xml`
- `mobile/app/src/main/res/values-fr/strings.xml`
- `mobile/app/src/main/res/values-hi/strings.xml`
- `mobile/app/src/main/res/values-km/strings.xml`
- `mobile/app/src/main/res/values-lo/strings.xml`
- `mobile/app/src/main/res/values-mg/strings.xml`
- `mobile/app/src/main/res/values-mn/strings.xml`
- `mobile/app/src/main/res/values-my/strings.xml`
- `mobile/app/src/main/res/values-ne/strings.xml`
- `mobile/app/src/main/res/values-si/strings.xml`
- `mobile/app/src/main/res/values-so/strings.xml`
- `mobile/app/src/main/res/values-sw/strings.xml`
- `mobile/app/src/main/res/values-ur/strings.xml`
- `mobile/app/src/main/res/values-bn/strings.xml`
- `mobile/app/src/main/res/values-pl/strings.xml`
- `mobile/app/src/main/res/values/strings.xml`
- `tools/support-tickets/support-tickets.js`
- `tools/test-payout-rules/moderation.test.js`
- `tools/test-payout-rules/package.json`

## Retention — approved 10 October 2026

- Block lists exist until unblock/account deletion. Account deletion removes outgoing
  blocks, incoming block references and all own moderationState documents.
- Open moderation reports remain until resolved/closed. Terminal reports expire after
  180 elapsed days from `resolvedAt` / `closedAt`, falling back to `statusUpdatedAt`
  for existing terminal records. Missing timestamps are reported and retained; no
  invented/backfilled dates. Reopening is not supported by the admin tool.
- `retentionHold.active=true` postpones deletion for legal/security obligations.
  Releasing a hold never changes the resolution/closure timestamp or expiry.
- Use `node tools/support-tickets/support-tickets.js <env> moderation-hold <id> on "reason"`
  and `off "reason"` to release. This admin-only CLI records actor, time and reason.
  Client writes remain denied. Do not mutate holds directly during cleanup.
- A transaction claims eligible reports via `retentionDeletionStartedAt`, serializing
  with the hold command. Once claimed, new holds and admin replies/resolution are
  rejected. All descendant collections (including nested/orphaned history) are
  recursively removed before deleting the parent. Partial failures leave the parent
  for the next scheduled retry. No TTL is used.
- New function `cleanupModerationAndLegalData`: daily `30 3 * * *`, UTC,
  us-central1, existing deployment trigger 580. It also purges deleted accounts'
  legalAcceptances after five calendar years; see ACCOUNT_DELETION.md.
- Payout reports, payment data and historical game UIDs are not cleanup targets.

The owner confirmed on 10 October 2026 that reports will be regularly reviewed and
appropriate moderation action taken. This is an ongoing operational responsibility.

Verification for this change: Firestore emulator suite 28/28, account-deletion
Firestore/RTDB emulator suite 5/5, support-ticket unit tests 2/2 passed.
No deployment, publication or live data changes.
