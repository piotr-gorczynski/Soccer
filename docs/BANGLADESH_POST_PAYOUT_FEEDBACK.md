# Bangladesh post-payout feedback messages

## Design and destination

Uses existing `supportTickets/{ticketId}/messages`, owner-scoped conversation
callables, unread reply versions/envelope badge, and generic support push.
There is no second inbox or automatic payout-completion feedback request.

Facebook inspection on 19 November 2026 found the bilingual Bangladesh launch
post and older tournament posts, but no verified post for the current Bangladesh
Kickoff Cup. Keep the existing Facebook Community destination. No Facebook post
was created or edited. Proposed future workflow: an administrator publishes a
relevant tournament/winner post, then explicitly configures its exact URL.

`firebase/functions/support-tickets/community.json` is the single authoritative
Community URL. Gradle generates `R.string.facebook_community_url` from it for
Settings; the admin tool reads the same file. The destination itself is unchanged.
Per-tournament `postPayoutFacebookPostUrl` overrides it only when explicitly set
to a validated Facebook post/permalink. Missing configuration uses Community;
invalid configuration fails closed rather than guessing a post URL.

## Admin commands

Run from repository root, with existing environment service-account credentials:

```powershell
node tools/support-tickets/support-tickets.js dev send-post-payout-feedback-message PAYMENT_ID --dry-run
node tools/support-tickets/support-tickets.js dev send-post-payout-feedback-message PAYMENT_ID
node tools/support-tickets/support-tickets.js dev send-post-payout-feedback-message PAYMENT_ID --language en
node tools/support-tickets/support-tickets.js dev set-feedback-facebook-post TOURNAMENT_ID "https://www.facebook.com/PAGE/posts/POST_ID"
node tools/support-tickets/support-tickets.js dev set-feedback-facebook-post TOURNAMENT_ID clear
```

Use `test` or `prod` explicitly for those environments. No real send is part of
implementation/testing. `--dry-run` reads/validates and previews without writes
or notification. Only `completed`, BDT, Bangladesh tournament payouts and active
existing player profiles are accepted. No private recipient document is read.

The deterministic ticket ID is `feedback_` plus SHA-256 of the payment ID.
The Firestore transaction reads the payout, tournament, user and existing ticket
before any writes. Concurrent/repeated commands conflict on the same ticket;
only the first succeeds by default. An intentional `--force` appends another
message/version in that conversation, preserves previous messages/read version,
and is audited. It never changes payout status/results/prize/recipient details.

The conversation starts resolved, with a support-authored message, no invented
player message, reply version 1 and read version 0. The player may reply/reopen
using the existing confirmation and rate limits. A resolved message does not
create an open support request or hold payout retention. Existing retention
rules remain unchanged; this feature adds no independent retention policy.

The existing `onSupportTicketCreated` trigger routes these new outbound tickets
to the same notification handler used by `onSupportTicketUpdated`. Notification
leases keyed by reply version prevent duplicate push on event retries. Subsequent
forced messages use the existing update trigger. Push is best-effort: missing or
invalid FCM token can prevent delivery; inbox/unread state remains available.
Lock-screen payload contains only the existing generic support notification.

## Templates and links

`feedback-templates.json` contains editable EN/BN templates using `{tournament}`
and `{url}`. Profile language `bn` selects Bengali; other languages fall back to
English. Admin may explicitly choose `--language en|bn`. No positive-review,
star-rating or proof request; feedback is optional and unrelated to prize or
future participation. Templates contain only public tournament name and URL.

BD support-authored message bodies recognize HTTPS URLs for approved hosts:
Facebook, Google Play, YouTube and the developer website. Other URLs, HTTP,
credentials in URLs and nonstandard ports remain plain selectable text. No HTML
is rendered. Links use Android `ACTION_VIEW`, letting the OS choose browser/app.
User-authored messages and Global retain their existing plain-text behavior.

Feedback messages include only validated public metadata in the conversation
API: `kind=post_payout_feedback`, `feedbackUrl`, `destinationType`. Existing plain
messages need no migration. Payout account numbers, transfer references, admin
identity and audit details are not added to the API projection.

Only tapping the matching feedback URL in BD emits
`post_payout_feedback_link_click` with `destination=facebook`,
`source=post_payout_message`, `destination_type=community|tournament_post`.
No UID, nickname, payment ID, tournament name or URL is sent in this event.
Opening/reading a message emits no link-click event. Facebook posting/commenting
is neither checked nor tracked. No GA4 custom definitions are created.

## Rollout

Requires a future Android release for clickable links/analytics. Old clients can
still read the plain-text message. Deploy changed support functions using the
existing YAML/trigger 580 through DEV, TEST and PROD, verifying source branch and
environment. Deploy is needed for first-message push and public link metadata.
No new cloud function or Firebase rule/index is required. Production sends remain
explicit administrator actions after rollout validation.

## Validation

33 Node tests passed across outbound admin messages, existing support commands,
conversation projections, attention routing and notification idempotency.
Android focused suites: BD 15 passed; Global 14 passed, 1 BD-only test skipped.
Both assemble_devBangladeshDebug and assemble_devGlobalDebug succeeded.
Android checks cover HTTPS validation, plain-text rendering, ACTION_VIEW routing,
actual-tap-only analytics and unchanged Global behavior. Node checks cover completed
BD payout gating, unavailable records, unchanged payout data, EN/BN templates,
private-field exclusion, preview, concurrent deduplication/intentional override,
destination validation and generic push payloads.

Initial automated evidence: mobile/build/post-payout-node.log and post-payout-feedback.log
(ignored build artifacts). Subsequent DEV testing used a simulated payment with no
funds transferred and an explicit feedback send. The player confirmed receipt and
successful navigation through the Facebook link on the physical device. An approved
tournament Facebook post was subsequently published and mapped explicitly in PROD
and DEV; those mappings are Firestore data, not hardcoded destinations in the app.

Release 17.30 packages these changes. Deploy trigger 580 to the target environment
for the backend changes if not already present there; Android-only UX changes do
not require that deployment. The release preparation does not initiate a deployment,
send another feedback message or transfer money.
