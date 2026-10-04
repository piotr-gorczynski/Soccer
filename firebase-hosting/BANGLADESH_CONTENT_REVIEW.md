# Bangladesh English website draft

Prepared 2026-10-01. The five pages in `public/bangladesh/` are local review drafts, not approved legal documents. No hosting deployment has been performed. The 2026-10-03 change prepares Bangladesh-only privacy links; do not release that build before publishing the reviewed page.

## Pages

- `index.html`: overview, 18+ requirement, shared accounts and installed-variant restriction.
- `tournaments-en.html`: registration, tournament-specific rules, payout statuses and support.
- `terms-en.html`: participation and payout terms draft.
- `privacy-en.html`: Bangladesh-specific data handling and current deletion limitations.
- `support-en.html`: payout help, support contact and deletion requests.

Each page has a draft banner and `noindex`. These are not access controls. A full Firebase Hosting deployment would publish the files; complete the review before deploying them. Existing Global pages and hosting configuration have not been changed.

## Implementation used as evidence

- `firebase/functions/join-tournament/eligibility.js`: recorded eligibility declarations and assigned regulation; not identity or geographic verification.
- `tools/create-regulation/regulation-example.json`: example rules. Amounts, methods, schedules, eligibility, tie handling and prize allocation must remain defined by each tournament's assigned rules, not a fixed website promise.
- Payment workflow and support implementation: manual handling, seven payment statuses, provider references, issue messages, recipient changes and handling history. A support reply does not itself confirm receipt of a payment.
- `firebase/functions/remove-account/index.js`: authentication deletion and selected profile-field removal; associated payment/support/gameplay records are not comprehensively deleted.
- Android `TermsActivity.java`: currently opens the existing root terms pages and stores shared acceptance fields. These draft Bangladesh pages are not yet connected to that flow.

## Resolve before publication

1. Define and implement the process for deleting remaining associated personal data. Set retention periods and identify any justified exceptions. Do not describe the current account-removal function as complete data deletion.
2. Confirm actual production transfer providers, their privacy-policy links and the information disclosed to them. Do not promise a provider, payment deadline or support response time that operations cannot guarantee.
3. Confirm the support mailbox and the external account/data-deletion request process are operational.
4. Review the terms/privacy text, set effective dates and align Play Console Data safety and deletion declarations with actual behavior.
5. Keep the shared general Terms URL and acceptance for both flavors (owner decision). Tournament-specific prize rules are accepted when joining. Make payout privacy information available before collecting recipient details; separate Bangladesh general-Terms acceptance is not a launch requirement.
6. Verify Google Play country targeting when publishing the Bangladesh app. The draft describes the intended targeting strategy, not an audit of current Console configuration.
7. Remove draft notices and editorial publication notes only after these decisions are resolved. Decide when indexing should be enabled.

Relevant Google Play guidance: [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en) and [account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en). Preparing these pages alone does not establish compliance.

## Remitly testing update — 2026-10-02

Owner-reported real tests: bKash 320 BDT delivered (verified); Rocket Personal 320 BDT
sent/in progress (not verified delivered). Both required firstName and lastName separately.
Nagad is excluded: Remitly requires a transfer-purpose classification, and the available
Family support / Savings / Payment for service choices do not accurately represent a tournament prize.
These details are internal evidence, not player-facing copy or prize-payment legal approval.

Current supported wallets: bKash and Rocket. Manual Remitly processing remains in place, with no API
integration and no new tax withholding. Bangladesh withholding clarification from NBR remains pending.
Current recipient data moves to payments/{id}/private/recipient; no historical phone numbers are
scrubbed. Retention periods and deletion operations remain unresolved, as above.

The shared general Terms URLs and acceptance are intentional: the owner confirmed that general
Terms do not differ between flavors. The Bangladesh Terms draft is review material, not a requirement
for a second general agreement. Tournament rules handle prize conditions; payout privacy information
must describe the additional data collected from winners. Do not deploy these drafts as final.

Owner clarification (2026-10-02): Bangladesh has never been released to production. No backward
compatibility with old payout clients/schema is required. The app only reads private recipient data;
existing test data/history is not migrated or deleted. Deploy backend/rules before the first BD release.


## Privacy implementation review — 2026-10-03

Policy draft: `public/bangladesh/privacy-en.html`, independent version `BD-privacy-2026-10-03`.
Intended URL: https://piotr-gorczynski.com/bangladesh/privacy-en.html . English is explicit;
no translated policy is implied. Bangladesh Account, Terms and payout screens link to it.
Root privacy links followed inside the Bangladesh Terms WebView are redirected to it.
Global URLs/content are unchanged. General Terms acceptance remains intentionally shared
(`termsAccepted`, `termsAcceptanceDate`, `language`); it is not privacy consent or a versioned
privacy acceptance. No privacy acknowledgement existed and none is fabricated by opening a link.
Bangladesh privacy and Global privacy can evolve independently at their separate URLs.

### Current storage and access

| Information | Current location | Purpose/access |
|---|---|---|
| Separate `firstName`, `lastName`, `walletNumber`, `walletProvider`, `submittedAt` | `payments/{id}/private/recipient` | Current payout recipient; owner reads, authenticated owner batch writes under status/rank/provider rules; authorized Admin SDK access |
| Winner `userId`, `tournamentId`, amount, currency, status, processing/status timestamps, `transfer.provider`, `transfer.providerReference`, `transfer.sentAt`, `transfer.completedAt` | `payments/{id}` | Prize and transfer evidence; owner reads, constrained owner submission, admin status changes |
| Transition, actor, timestamps, schema version, previous issue, administrator input | `payments/{id}/statusHistory/{event}` | Backend/admin only; new recipient events exclude structured names/numbers |
| Support initial text, latest reply, category, app context, status | `supportTickets/{id}` | Owner read, backend/admin writes |
| Support messages, handling history, notification bookkeeping | ticket `messages`, `statusHistory`, `notificationEvents` | Owner reads messages; history/bookkeeping backend/admin only |
| Tournament eligibility, results/rankings | tournament participants/results | Visible under authenticated tournament rules; no new structured recipient fields copied here |

### Findings and minimum-data follow-up (not executed)

1. No automatic recipient expiry exists. Completing/cancelling a payment and removing an account
   leaves private recipient data. The policy explicitly discloses this gap; final publication remains
   pending an operational deletion process. Do not describe the draft as compliance certification.
2. Keep operational recipient details only while payment/correction/dispute handling needs them or
   a documented legal hold applies. Proposed process: review a terminal payment and related tickets;
   record hold purpose/review date or approve erasure; then remove private recipient details and record
   a metadata-only erasure event. No arbitrary duration, TTL, automatic purge or live deletion added.
3. Long-term design: retain only tournament/winner internal ID, amount/currency, wallet provider,
   transfer provider/reference, payment dates/status and essential audit metadata. The current parent
   does NOT retain wallet provider separately from the private document. Before recipient erasure,
   add a minimized provider snapshot through a reviewed schema change. Do not add names/full number;
   last digits are optional and not implemented. Internal IDs and references remain personal data.
4. Existing historical `recipientInfo`/account numbers may remain in old parents/history. No historical
   numbers were inspected live, edited or removed. Proposed migration requires a read-only inventory,
   reviewed field list, legal-hold decisions and owner approval before redacting historical data.
5. Free text duplicates remain: initial support message is in ticket root, `messages/initial` and
   `statusHistory/created`; replies and administrator input can recur in root/history/messages.
   Payment `issue`, `adminNotes`, history `details` and `previousIssue` may contain volunteered wallet
   details. Future design: store conversation text once in messages; history/root reference message IDs;
   keep only structured reason/status metadata. No existing conversation is silently rewritten.
6. Payment/support FCM payloads use fixed text and IDs, not wallet fields or conversation bodies.
   Reviewed payout UI and Analytics events do not log entered recipient fields. CLI support tables
   print user/admin text, so any voluntarily supplied personal information can reach terminal logs.
   Operator guidance: do not paste full names/numbers into notes, messages, logs or references.
7. Existing Firestore rules restrict private recipients to the owning user and deny other-player
   access. Rules unchanged. Admin SDK bypasses client rules; IAM least-privilege work remains separate.
   General `users` profiles remain readable by signed-in users, including non-public profile fields;
   this existing broad profile access needs separate review and must not be described as field-level privacy.
8. Copy PROD/TEST/DEV tools copy private recipient data and histories verbatim; test-environment copies
   are additional sensitive-data copies. Any future erasure policy must cover them, exports and backups.
9. `removeAccount` deletes Auth and selected email/Facebook profile fields, replaces nickname and sets
   `accountDeleted`; it does not erase all profile identifiers, presence, recipient/history/support data.
   No deletion function or retention automation changed in this task.

### Legal review sources and unresolved assumptions

Official Bangladesh Personal Data Protection Act 2026:
- [Section 18](https://bdlaws.minlaw.gov.bd/act-1692/section-57284.html): retention subject to prescribed periods.
- [Section 19](https://bdlaws.minlaw.gov.bd/act-1692/section-57285.html): subject to section 18, processing-related
  records retained in a register for at least five years unless otherwise specified; prescribed updating procedure.
- [Section 1](https://bdlaws.minlaw.gov.bd/act-1692/section-57267.html): scope and staged commencement.

These provisions do not establish in this review that every full wallet number must be retained five
 years. Confirm applicability to this Poland-based organizer, implementing regulations, required register
contents, retention start/end dates, lawful bases, cross-border safeguards and Polish/EU obligations with
qualified advice. Existing payment history is not asserted to be a complete statutory processing register.
The proposed separation/minimization is a design interpretation, not a binding legal conclusion.
Provider notices linked in the policy: official Google, Meta, Remitly Poland English, bKash, Rocket.
No tax logic, Remitly API, Nagad support, data migration, deployment, commit or push introduced.


### Verification — 2026-10-03

- `_devGlobalDebug` and `_devBangladeshDebug` assemble: PASS after final string changes.
- `PrivacyPolicyLinksTest`: 2/2 PASS per variant (4 total): correct destination, flavor visibility,
  root privacy URL matching without matching Terms/foreign-host URLs. Robolectric activity explicitly
  models the package name because its default test package differs from the Android application ID.
- Existing payment workflow tests: 7/7 PASS, including exclusion of private and legacy recipient
  fields from new structured submission events.
- Existing Firestore emulator suite: 10/10 PASS, including owner isolation and atomic recipient writes.
- HTML local links and Android XML parsing: PASS; new link label present in all 20 supported languages
  and explicitly identifies the English policy. The policy itself is English only.
- Global `privacy-en.html` unchanged; no Firestore rule changes or live-data operations.
- An initial Gradle command accidentally ran the full Global suite (320 tests, 49 failures outside
  this change, matching previously documented resource/UI failures). Focused tests and builds above
  passed; full-suite success is not claimed. No device test or deployed-URL verification performed.


## Retention implementation supersedes earlier gap notes — 2026-10-04

The owner selected 180 days for raw recipients and five calendar years for minimized payment/audit
records after completed/cancelled, suspended by unresolved support tickets or an administrator hold.
This is now implemented in `update-payment-status/retention.js`, with daily cleanup and admin hold
controls. See BANGLADESH_PAYMENT_FLOW.md for exact boundaries, scope, deployment requirements and
legacy exceptions. The raw-recipient automatic-deletion gap above is resolved in source code, but
nothing is deployed. Existing statusHistory is neither removed nor migrated; unmarked old history,
support conversations, backups and copies remain outside cleanup. New payment history omits free
text; new support history references messages. Shared Terms and Global privacy are unchanged.
The policy draft is now BD-privacy-2026-10-04. Retention periods are owner-selected, not a legal
conclusion that raw wallet numbers must be retained five years.
