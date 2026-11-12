# Help Centre rollout

Shared by Global and Bangladesh. Work is phased so the backend can be tested
before introducing a new user-facing screen or enabling moderation pushes.

## Phase 1 — conversation backend and operator tools

Deployed to DEV through trigger 580, build
`fafacdd8-7d1b-4852-917c-488a1ac71824`, from commit `160f9c9e`.
All four new callables are ACTIVE and reject unauthenticated requests. Existing
supportTickets and messages remain authoritative. No TEST/PROD deployment was
performed as part of this rollout.

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

Implemented in the shared Android source for Global and Bangladesh:

- Envelope action beside Account, 48dp touch target, red badge hidden at zero,
  localized digits/99+ display and a spoken description with the full count.
  Offline and invite-warning items remain independent. The envelope stays
  available, dimmed, when offline, so the screen can explain the failed load.
- HelpCentreActivity lists own reports/statuses/dates/unread counts with paging;
  SupportConversationActivity shows history, earlier pages, reply input and
  explicit confirmation before reopening a resolved conversation. Closed threads
  have no composer. Empty and whitespace-only submissions are disabled.
- The envelope is visible only for Firebase-authenticated users, including
  anonymous accounts. Auth changes reset the badge and close/blank old screens;
  callback generations prevent responses for another account or stopped screen
  from repopulating the UI. Anonymous accounts see an account-link reminder.
- Badge requests are asynchronous and do not block Menu's loading overlay. The
  active Menu polls at most once per minute and refreshes on foreground return;
  open support screens refresh every 30 seconds. No polling runs when stopped.
  The existing BackendServiceChecker gates screen requests, with network-change
  recovery and retry UI. Backend error text is mapped to localized labels and is
  not displayed or logged verbatim.
- A conversation marks replies up to the latest displayed support response read
  only after that response is shown inside the scroll viewport for 400ms with
  window focus and an online session. A hidden ancestor, background screen,
  offscreen response or failed load cannot acknowledge it. Reading the inbox or
  fetching a badge never marks anything read. A successful server receipt is
  required; failed receipts retry only after a new load. This is a UI visibility
  signal, not proof that the person read every word.
- A private account/ticket-scoped draft stores text and a stable UUID/payload.
  The idempotency key is durably committed before sending. Ambiguous failures
  retain that payload, lock editing and offer explicit retry; definite rejection
  keeps editable text. Reloading an already delivered message can confirm success
  without resending. No automatic offline send queue or message Analytics exists.
  Drafts for a different UID are never restored, and active logout/account change
  clears the previous owner's drafts.
- All 26 new labels/descriptions are translated into the existing 20 languages,
  including Bengali, Somali and Malagasy. Message bodies remain as authored.
  Existing BaseActivity language and edge-to-edge handling apply to these screens.

Phase 2 changes only Android code/resources. No additional trigger is needed on
DEV. Before a TEST/PROD app release, deploy Phase 1 there with trigger 580 first.

### Device checklist for Phase 2

Automated validation (2026-11-11): 27 focused Android tests passed for each
of DEV Global and DEV Bangladesh (54 total): draft durability/isolation,
callable payload boundaries, foreground unread polling, account changes,
offline/focus/viewport read guards, pagination, reply retry/idempotency,
closed conversations and explicit reopening. Both DEV debug APKs built
successfully. Robolectric tests inject the OS foreground window signal;
physical-device layout, RTL, large-font and real server round-trip checks below
remain required. Production retains the actual window-visibility check.

1. Run the DEV Bangladesh or Global debug build and sign in with a disposable
   test account. The envelope should be beside Account; verify it alongside the
   offline indicator and blocked-invite warning, including RTL/large fonts.
2. Open the envelope. An empty account shows an explanation. Submit a player
   report through the existing flow (or a payout support ticket on BD); the new
   conversation should appear in the inbox without marking replies read.
3. Find the test reference using `node tools/support-tickets/support-tickets.js dev list`.
   Send an operator test response with the existing `dev reply` command. Return
   to Menu or wait up to 60 seconds; check the unread badge.
4. Open the conversation. Verify the response and its timestamp. Leave it out of
   view or background the Activity and confirm it is not acknowledged. Bring the
   latest response into view; then return to Menu and check the server-updated
   count. Use the whole-conversation `dev view` command to check messages.
5. Reply, check empty-input prevention and explicit reopening of resolved
   conversations. Closed conversations must not offer sending.
6. Test offline load/recovery, interruption during sending and process recreation.
   Retry must use the existing request ID and create one server message. Sign in
   with another test account: it must not show the first account's count, messages
   or draft. Account deletion/reinstallation is not required for this test.

Push routing remains Phase 3. Phase 2 receives changes by foreground refresh,
not by newly enabled moderation notifications.

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
