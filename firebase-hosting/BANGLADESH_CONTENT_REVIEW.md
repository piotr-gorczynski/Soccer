# Bangladesh English website draft

Prepared 2026-10-01. The five pages in `public/bangladesh/` are local review drafts, not approved legal documents. No hosting deployment or application URL change is part of this work.

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
