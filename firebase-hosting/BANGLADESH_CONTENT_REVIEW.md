> **Current status — 9 October 2026:** `BD-privacy-2026-10-09` is the effective Bangladesh Privacy Policy, effective 9 October 2026, in English and Bengali. This finalizes the status/version of the 7 October revision without changing its substantive content. The owner confirms that the Bangladesh website pages have been published; this status update does not deploy the revised labels. No numeric legal-acceptance retention period is introduced. See [ACCOUNT_DELETION.md](../docs/ACCOUNT_DELETION.md). Earlier dated review notes below are historical and do not override this current status.

# Bangladesh English website content review

Prepared 2026-10-01; current status updated 2026-10-09. The effective Bangladesh Privacy Policy is BD-privacy-2026-10-09. This does not assert external legal approval. The following publication restrictions and readiness assessments record earlier reviews, not the current publication status.

## Pages

- `index.html`: overview, 18+ requirement, shared accounts and installed-variant restriction.
- `tournaments-en.html`: registration, tournament-specific rules, payout statuses and support.
- `terms-en.html`: participation and payout terms draft.
- `privacy-en.html`: Bangladesh-specific data handling and current deletion limitations.
- `support-en.html`: payout help, support contact and deletion requests.

The Privacy Policy has an effective date/version rather than a draft banner. Only the proposed Bangladesh Terms retain a draft banner; `noindex` is not an access control. The approved publication scope is only `bangladesh/privacy-en.html` and `bangladesh/styles.css`; the other Bangladesh pages must remain unpublished. Use the existing Firebase Hosting CLI against `soccer-ads-hosting`, with a temporary staging directory preserving the live release files byte-for-byte and adding only those two files. Do not deploy the full repository public directory while it contains unapproved drafts. Global pages and hosting configuration remain unchanged.

## Publication readiness review — 6 October 2026

No Hosting deployment, commit, backend/Android change or data mutation is part of this review. At that review, `BD-privacy-2026-10-06` was the effective Privacy Policy and was unchanged; it has since been superseded by `BD-privacy-2026-10-09`. Its limited publication already succeeded; the scope paragraph above describes that previous release, not approval to publish more files now.

| Page | Status | Remaining blocker |
| --- | --- | --- |
| `index.html` | READY TO PUBLISH | None for this informational page; Google Play availability is not promised. |
| `tournaments-en.html` | READY TO PUBLISH | None for this generic guide; each real tournament needs its own active rules. |
| `support-en.html` | READY TO PUBLISH | None; app labels and deletion limitations match implementation. |
| `terms-en.html` / `terms/BD-terms-2026-10-06.html` | PREPARED, NOT PUBLISHED | Versioned app acceptance implemented locally. Review text/privacy notice, publish immutable documents and deploy rules before app release. The unversioned page remains a draft. |

### Versioned Terms acceptance — implementation prepared 7 October 2026

The owner chose separate Terms scopes. This supersedes earlier shared-Terms decisions recorded below; it does not publish or make the prepared Terms effective.

- Bangladesh requires **BD-terms-2026-10-06**, English, at `https://piotr-gorczynski.com/bangladesh/terms/BD-terms-2026-10-06.html`. The immutable file is prepared locally; the unversioned editorial draft remains unpublished.
- Global new acceptance references byte-preserved versioned snapshots of the current documents dated **2025-07-30**, under `/terms/GLOBAL-terms-2025-07-30/{language}.html`. This date does not prove what existing users accepted historically.
- `TermsPolicy`, `TermsRepository` and `TermsActivity` store immutable owner-only records at `users/{uid}/legalAcceptances/terms_{scope}__{version}` with server time, document language/URL/SHA-256 and app metadata. HTML is hash-verified before acceptance. Failed writes do not permit continuation; checks are server-only.
- Global retains explicitly bounded legacy eligibility, without fabricating records. Bangladesh never relies on `termsAccepted`. Deprecated compatibility fields remain for old apps. Tournament declarations remain separate.
- See [TERMS_ACCEPTANCE.md](../docs/TERMS_ACCEPTANCE.md) for the model, hashes, tests, future version bump process and release ordering. Publish the immutable pages and necessary linked assets, then deploy approved rules before releasing the updated app; neither step has happened in this task.
- The effective Privacy Policy is unchanged. It needs reviewed wording for general-Terms evidence, purpose/retention and the move from shared to Bangladesh Terms. Do not silently apply payout-retention periods to legal acceptance records.

### Content and implementation checks

- bKash and Rocket: both verified by owner-reported successful Remitly deliveries; no personal names, wallet numbers, references or amounts from those tests are included in the pages. Nagad is not offered.
- `tools/create-regulation/regulation-example.json`: BD, minimum age 18, bKash/Rocket, free entry, skill-based deterministic ranking, tie/prize rules. Prize amounts stay in assigned regulations, not website promises.
- `tools/create-tournament/tournament-config-bd.json` refers to a regulation that **does not exist in PROD**, and the read-only PROD query found **zero regulations with market BD** on 2026-10-06. Creating an appropriate active PROD regulation is a tournament-launch prerequisite, not a blocker to the generic informational pages. No regulation was created or copied.
- `payment-workflow.js`: all seven statuses represented; Sent is not confirmed delivery. Manual processing through Remitly is operational practice, not an automatic API transfer or fixed-time promise.
- Support UI displays `waiting_for_user` as **Support replied**, alongside Open, Resolved and Closed. Resolution is not proof of payout delivery. Users can report a remaining problem again or contact support; no in-ticket reply capability is promised.
- `retention.js` and `remove-account/index.js`: 180-day raw/five-calendar-year minimized retention, holds and incomplete immediate account erasure reflected consistently; the effective Privacy Policy remains the detailed source, including legacy/support exceptions.
- Neutral tax clause added only to proposed Terms: participants handle applicable taxes except organizer obligations to withhold/remit. Section 118 applicability and organizer withholding/reporting responsibilities still require external clarification. No 25% rate, exemption or legal approval is asserted. This review does not settle that question.

### Navigation and next publication steps (not executed)

1. Approve publication of the three READY pages. Keep `bangladesh/terms-en.html` excluded while its acceptance blocker remains.
2. Review/commit only approved website changes to `origin/prod` when requested. Do not include unrelated files.
3. Use the existing Firebase Hosting CLI and `soccer-ads-hosting` production site. Build a temporary staging directory from the latest live manifest, verify hashes, preserve all live files/configuration, and add only the three approved pages. Keep the live policy and stylesheet unchanged. Never deploy the whole repository `public` directory while the Terms draft is present.
4. The three prepared pages link to `/terms-en.html` (shared Terms), each other, and the effective Bangladesh policy, so they can be published together independently of the proposed Terms.
5. The live policy's existing Home/shared-Terms/email navigation remains valid. In a separately approved edit at joint publication, it may gain links to `index.html`, `tournaments-en.html` and `support-en.html`. Do not point it to the proposed Terms until those are approved and effective. No policy navigation change is made now.
6. Deploy only Hosting with explicit project/account/config; then verify HTTP responses, content and links, unchanged live-policy/Global hashes, and absence of the draft Terms from the release manifest. Do not deploy functions, rules, indexes or Android.

## Implementation used as evidence

- `firebase/functions/join-tournament/eligibility.js`: recorded eligibility declarations and assigned regulation; not identity or geographic verification.
- `tools/create-regulation/regulation-example.json`: example rules. Amounts, methods, schedules, eligibility, tie handling and prize allocation must remain defined by each tournament's assigned rules, not a fixed website promise.
- Payment workflow and support implementation: manual handling, seven payment statuses, provider references, issue messages, recipient changes and handling history. A support reply does not itself confirm receipt of a payment.
- `firebase/functions/remove-account/index.js`: authentication deletion and selected profile-field removal; associated payment/support/gameplay records are not comprehensively deleted.
- Android Terms implementation: see the 7 October versioned acceptance section above. Deployed older apps still use shared legacy fields until a new release.

## Resolve before publication

1. Define and implement the process for deleting remaining associated personal data. Set retention periods and identify any justified exceptions. Do not describe the current account-removal function as complete data deletion.
2. Confirm actual production transfer providers, their privacy-policy links and the information disclosed to them. Do not promise a provider, payment deadline or support response time that operations cannot guarantee.
3. Confirm the support mailbox and the external account/data-deletion request process are operational.
4. Review the terms/privacy text, set effective dates and align Play Console Data safety and deletion declarations with actual behavior.
5. Separate Bangladesh general Terms are now required in the prepared app. Complete the versioned document/rules/app rollout and privacy review described above. Tournament-specific prize rules remain independently accepted when joining.
6. Verify Google Play country targeting when publishing the Bangladesh app. The draft describes the intended targeting strategy, not an audit of current Console configuration.
7. Remove draft notices and editorial publication notes only after these decisions are resolved. Decide when indexing should be enabled.

Relevant Google Play guidance: [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en) and [account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en). Preparing these pages alone does not establish compliance.

## Remitly testing update — status confirmed 2026-10-04

Owner-reported real tests: bKash — VERIFIED / DELIVERED (320 BDT); Rocket — VERIFIED / DELIVERED
(320 BDT). Both were real successful Remitly transfers reaching final status Delivered. The Rocket
transfer from Poland to Bangladesh was received in the recipient account, confirming the tested
flow end-to-end. The earlier unsuccessful Rocket attempt used an organization-style recipient name
and was paused; the successful test used the recipient's personal name matching the Rocket account.
Accurate recipient identity is essential and supports separate firstName, lastName, walletProvider
and walletNumber fields. No recipient names, numbers, transfer references or screenshots are recorded.
Nagad is NOT SUPPORTED / EXCLUDED: Remitly requires a transfer-purpose classification, and the available
Family support / Savings / Payment for service choices do not accurately represent a tournament prize.
These details are internal evidence, not player-facing copy or prize-payment legal approval.

Current supported wallets: bKash and Rocket. Manual Remitly processing remains in place, with no API
integration and no new tax withholding. Bangladesh withholding clarification from NBR remains pending.
Current recipient data moves to payments/{id}/private/recipient; no historical phone numbers are
scrubbed. Retention periods and deletion operations remain unresolved, as above.

Historical note (superseded by the 7 October versioned Terms implementation): the shared general Terms URLs and acceptance were intentional: the owner confirmed that general
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
Historical pre-versioning state (superseded above): Global URLs/content are unchanged. General Terms acceptance was shared
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
legacy exceptions. Retention is deployed in DEV, TEST and PROD as of 2026-10-06. DEV retention tests and the TEST smoke test passed. PROD functions and the daily 03:00 UTC scheduler were verified active; no manual PROD cleanup test was performed. Existing statusHistory is neither removed nor migrated; unmarked old history,
support conversations, backups and copies remain outside cleanup. New payment history omits free
text; new support history references messages. Shared Terms and Global privacy are unchanged.
The effective policy is BD-privacy-2026-10-09 (9 October 2026). Retention periods are owner-selected, not a legal
conclusion that raw wallet numbers must be retained five years.
