# Bangladesh Prize Payment Flow

**Status:** Implemented recipient schema v2; not deployed by this task
**Last updated:** 2026-10-02

## Product rules and observed transfer results

Participation is free. No entry fee, stake, wager, deposit, purchase or payment is required from
players. Prizes are organizer-funded and determined by the assigned tournament regulation.
Transfers are manually processed through Remitly; there is no automated Remitly API integration.
Supported wallets are **bKash and Rocket**. Regulation codes remain lowercase `bkash` / `rocket`;
recipient `walletProvider` uses uppercase `BKASH` / `ROCKET`.

Owner-reported real tests (recorded 2026-10-02):
- bKash: 320 BDT delivered to a public donation wallet. VERIFIED / DELIVERED, supported by Remitly
  In progress and Delivered emails.
- Rocket: 320 BDT submitted to a public donation Personal wallet. SENT / IN PROGRESS, not verified
  delivered. Recipient and transfer were accepted without a Reason for Sending prompt.
- Nagad is excluded. Remitly requires a transfer-purpose classification for Nagad, and the available
  classifications do not accurately represent a tournament prize (Family support, Savings, Payment
  for service). Never use a false purpose. This explanation is internal, not player-facing copy.

Both tested wallets required separate first and last names matching the wallet/Remitly recipient.
Donation tests are operational evidence, not legal approval for tournament prizes. Bangladesh
withholding obligations await NBR clarification. No tax withholding is introduced.

## Status and owner actions

| Stored status | Meaning | Owner action |
|---|---|---|
| `awaiting_details` | Prize exists; complete recipient details needed | Submit all four fields |
| `ready_for_processing` | Details submitted; waiting for manual processing | Read only |
| `processing` | Administrator is arranging the transfer | Read only |
| `sent` | Transfer submitted/in progress; not delivery confirmation | Wait |
| `completed` | Provider confirmed delivery | Read only |
| `action_required` | Details need correction | Correct and resubmit |
| `cancelled` | Administrator cancelled with a documented reason | Contact support if needed |

Normal path: awaiting_details → ready_for_processing → processing → sent → completed.
From ready_for_processing, processing or sent, an administrator may request correction through
`action_required`; a valid owner submission returns it to ready_for_processing. Existing cancellation
and terminal-state rules in payment-workflow.js remain unchanged. A support reply alone neither
completes the payment nor proves that its recipient details have been corrected.

## Current Firestore model

```javascript
// payments/{paymentId}: existing prize, owner, tournament, amount, currency and transfer fields
{
  status: "ready_for_processing",
  recipientDetailsVersion: 2,
  statusUpdatedAt: Timestamp,
  updatedAt: Timestamp,
  // transfer.provider, providerReference, sentAt, completedAt are added by administrators.
  // No firstName, lastName or walletNumber in this parent for new submissions.
}

// payments/{paymentId}/private/recipient
{
  firstName: "recipient first name",
  lastName: "recipient last name",
  walletProvider: "BKASH", // or ROCKET
  walletNumber: "01XXXXXXXXX",
  submittedAt: Timestamp
}
```

Names are required, trimmed, at most 100 characters, and are not split from a combined field or
inferred from a public nickname. The client explains that they must match the wallet/Remitly details
and incorrect details can delay or prevent payment. bKash numbers have 11 digits beginning 01[3-9];
Rocket numbers have 12 including the check digit. Validation checks format, not wallet ownership or
actual account existence. International +880/880 and formatted input is normalized.

A Firestore batch writes the private document, removes legacy parent recipientInfo and the resolved
issue, sets recipientDetailsVersion to 2, and updates status/timestamps. Rules enforce owner, rank,
editable source status, required fields, current regulation provider and atomicity via getAfter.
Only the owner can read the private document through client rules; administrators use authorized
Admin SDK/Console access. Other players and unauthenticated callers cannot read it.

New recipient submission audit events retain status, actor, timestamps and schema version, but no
structured names or wallet numbers. The existing trigger remains idempotent. Admin notes and support
messages are free text: operators must avoid copying recipient data into them. Separate storage
makes distinct retention possible; it does not itself implement automatic deletion.

## First production rollout

The owner confirmed that the Bangladesh flavor has never been released to production. Supporting
old Bangladesh clients or recipient schemas is therefore not required. Only the private recipient
schema is read; new audit events never copy recipient details, including when processing old test
events. The schema version identifies the format, not a compatibility mode.

No existing database records or statusHistory documents are edited or deleted by this task.
Historical test records may still contain account numbers. There is no automatic migration or cleanup.

Before rollout:
1. Prepare reviewed BD regulations and localized rules with only `bkash` and `rocket` and all four
   required recipient fields. Publish new rules for new tournaments; do not silently change accepted
   rules of existing test events. Obsolete test fixtures must be reviewed/reset separately if needed.
2. Deploy the updated recipient-history function, joinTournament and Firestore rules before releasing
   the first Bangladesh build. There is no old production Bangladesh app to coordinate with.
3. Use new-format data for end-to-end tests on dev. Do not infer recipient names or automatically
   relabel/restart transfers from old test records.
4. Administrators read current recipient data from private/recipient. CLI status/reference fields
   remain unchanged.
5. Publish reviewed Hosting content separately. This task does not deploy functions, rules, data,
   apps or Hosting. Existing Global/non-cash behavior and Terms routing remain unchanged.

General Terms and their acceptance are intentionally shared between Global and Bangladesh, as
confirmed by the owner. Separate Bangladesh general-Terms acceptance is not required by this design.
Tournament prize rules are accepted before joining; payout privacy information must be available
before a winner submits recipient details. Do not redirect the app to unapproved review drafts.

## Retention and support

Keep current recipient details separate from longer-lived transfer evidence. No numeric retention
period is assumed, no new tax rule is inferred, and no historical phone-number purge is performed.
The current remove-account function does not delete these private records or all associated history.
Finalize justified retention periods and deletion processes before publishing a final privacy policy.
Support tickets continue to use their existing lifecycle and access rules.

## Verification

- Android validator tests: both providers, required first/last names, number and provider, normalization,
  unsupported/Nagad rejection, Unicode names and length bounds.
- Local Firestore emulator tests: required fields, ownership/read isolation, provider allowlist even
  under legacy rules, regulation constraints, batch atomicity, immutable submitted state and legacy-write rejection.
- Backend tests: normal lifecycle and correction, existing audit preservation and recipient-data exclusion from new events.
- Regulation/tournament tools: accepted provider metadata and rejected retired providers.
- Build and focused test both `_devGlobalDebug` and `_devBangladeshDebug`; manually test the form and
  real operator workflow before production. Build/test success does not confirm real Rocket delivery.

### Local verification results — 2026-10-02

- `_devGlobalDebug` and `_devBangladeshDebug` assemble: PASS.
- BangladeshPayoutAccountValidatorTest: 5/5 PASS in each variant.
- Node eligibility/payment/support/regulation/tournament/CLI suite: 40/40 PASS.
- Local Firestore rules emulator: 10/10 PASS.
- Full Global unit run (invoked before correcting the task-specific filter): 320 tests, 49 failures
  outside the payout validator, including resource/configuration and unrelated UI tests. The full
  suite is not green; those failures were not addressed by this payout change.
- No on-device form test, live database migration, new remittance or deployment was performed.
