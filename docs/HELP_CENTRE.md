# Help Centre rollout

Shared by Global and Bangladesh. Work is phased so the backend can be tested
before introducing a new user-facing screen or enabling moderation pushes.

## Phase 1 — conversation backend and operator tools

Implemented in source; not deployed by this task. Existing supportTickets and
messages remain authoritative. No live tickets are changed during development.

Validation (2026-11-10): 17 unit tests and 35 Firestore emulator integration tests
passed (conversation authorization, concurrency, unread races, rate limits,
moderation, retention and legacy payout reads/writes). The full shared run also
found a pre-existing Terms HTML exact-content comparison failure: CRLF versus LF
in the Bengali public and frozen pages. The same mismatch exists in HEAD and
disappears when only line endings are normalized. Legal HTML and hashes were not
changed. This known test failure is separate from the Help Centre suites.

Four authenticated callable functions run in us-central1. Anonymous Firebase
authentication is accepted. The account must have an active user document.
Ownership always comes from context.auth.uid, never request parameters. Missing
and foreign tickets have the same not-found response. Deleted accounts and
tickets claimed by retention cleanup cannot participate.

| Callable | Request | Result |
| --- | --- | --- |
| listSupportConversations | `{}` or `{cursor: ticketId}` | `conversations`, total `unreadCount`, `nextCursor` |
| getSupportConversation | `{ticketId, cursor?: messageId}` | `conversation`, `messages`, `initialMessage`, `nextCursor` |
| sendSupportMessage | `{ticketId, message, requestId, reopen?: boolean}` | `ok`, `messageId`, `alreadyExists` |
| markSupportConversationRead | `{ticketId, seenReplyVersion}` | `ok`, remaining `unreadCount` |

List pages contain up to 25 summaries, newest updated first. The initial
implementation reads all owner summaries to compute an exact badge, including
legacy responses without counters. At higher ticket volume this should become
a transactionally maintained aggregate; there is no claim that this is a
constant-cost global count. Refresh the list after updates rather than treating
its cursor as a frozen snapshot.

Conversation pages contain up to 50 stored messages, chronologically ascending
within each page. The first page contains the latest messages. nextCursor fetches
older messages. Equal timestamps use document IDs to avoid pagination gaps.
initialMessage is the original legacy report when no initial message document
exists; render it only once, at the beginning of the conversation. A legacy last
support response can also be returned as a synthetic display item. These are
read-only compatibility views, not writes or invented historical messages.

Public message fields are only messageId, authorType (user/support), message and
createdAt (ISO UTC or null). Summaries contain ticketId, reference, category,
status, createdAt, updatedAt, latestSupportReply, latestSupportReplyAt,
supportReplyVersion, unreadCount and canReply. No author email, raw UID, reported
player identity, private payment data, retention hold or internal audit record is
returned. No message text is logged by these handlers.

Raw moderation documents and all raw message documents are not client-readable.
The owner-only payout summary query used by released Android versions remains
allowed when paymentId is nonempty. All direct client writes, audit records and
rate-limit state remain denied. Rule tests include the exact legacy payout query.

Messages have 1–2000 characters. requestId is a fresh UUID-like 16–64 character
identifier (letters, digits, underscore or hyphen), retained for network retries
of exactly that payload. Reusing it with another payload is rejected. Sending
the same request concurrently writes one message and one audit event, and does
not charge the rate limiter again. Sending limits are at least five seconds
apart, ten per fixed UTC hour and thirty per UTC day, per account across tickets.
The user-owned gate is transactional and cannot be edited by a client.

User replies set status open. Resolved tickets require explicit reopen=true;
closed tickets cannot be reopened. A retry of an already accepted message does
not reopen a subsequently finished ticket. The original resolution date remains
in history; a later actual resolution sets its own resolution date. Retention
claims, payment deletion and sending serialize through their parent documents.
Payout replies touch supportActivityAt without changing payout execution/status.

The updated operator CLI increments supportReplyVersion atomically on every
support message, even identical text. The player's read version is monotonic and
bounded by the version actually displayed by the app. A concurrent newer reply
remains unread. Read receipts do not reorder tickets, trigger notifications or
claim that a message has been read while offline. No implicit read occurs on GET.
Legacy last replies count as version 1; the next CLI response preserves them in
messages if not already stored. No bulk migration or production rewrite is used.

`node tools/support-tickets/support-tickets.js <env> view <ticketId>` prints
available history for an administrator without marking anything read. Existing
reply/resolve/list commands remain. Upgrade operator checkouts before enabling
the app badge: old copies do not increment the new version field. Arbitrary
manual Firestore edits are not a supported operator workflow.

Existing attention alerts handle new user messages after commit. Moderation push
notifications remain disabled until Phase 3. Existing payout pushes also detect
a changed reply version, so repeated identical text can still notify.

Deployment later uses existing trigger **580** / deploy_support_tickets.yaml,
which includes the conversation module, unit tests, functions and Firestore
rules. No new IAM or standalone manual Firebase deployment is required.

## Phase 2 — Android inbox and conversation screens

Not implemented yet. Add the envelope beside Account in the Menu toolbar, with
a red unread-message count, independent of the conditional server-unavailable
icon. Hide the badge at zero. Available to authenticated users including anonymous
accounts; clear cached identity/count on logout or account switch.

Open a new Help Centre Activity, then a conversation Activity. Show status,
dates, messages, reply field, explicit reopening and retry/loading/offline states.
Mark a version read only after the corresponding response is loaded and visible,
never from a background refresh or failed request. Preserve the request ID and
draft through network errors and recreation. Refresh after returning to Menu;
unread total is a message count, not a count of unresolved tickets.

Use existing server availability handling. Localized labels and accessibility
descriptions are required in all supported locales. Avoid tiny toolbar targets;
check the envelope, Account and offline indicator together and in RTL layouts.
Display stored response text as authored; no automatic translation is implied.

## Phase 3 — notifications and end-to-end device validation

Not implemented yet. Route neutral support notifications to the correct Help
Centre conversation; do not route moderation to payout screens. Keep bodies free
of report content and reported-player identity. The inbox is authoritative if
push is disabled or lost. Recheck signed-in ownership after authentication and
when following a stale link. Consider compatibility gating for released clients
that do not yet have the Help Centre Activity.

Test both flavours, anonymous accounts, another account/device, unread races,
logout, process restart, offline recovery, duplicate taps/retries, reopening,
resolved/closed states and real push delivery. Then update release notes and
deploy via the existing environment promotion process.

## Deliberate exclusions

No player-to-player chat, attachments, marketing broadcasts, arbitrary operator
outbound messages or additional authentication providers. No new support intake
form in Phase 1: conversations originate in the existing player-report or payout
flows. The reported player cannot read the reporter's conversation. Reinstalling
or losing an unlinked anonymous account still risks losing access; UI reminders
about securing that account belong in Phase 2.
