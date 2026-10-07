# Bangladesh Prize Payment Flow

**Status:** Implemented recipient schema v2; not deployed by this task
**Last updated:** 2026-10-04

## Product rules and observed transfer results

Participation is free. No entry fee, stake, wager, deposit, purchase or payment is required from
players. Prizes are organizer-funded and determined by the assigned tournament regulation.
Transfers are manually processed through Remitly; there is no automated Remitly API integration.
Supported wallets are **bKash and Rocket**. Regulation codes remain lowercase `bkash` / `rocket`;
recipient `walletProvider` uses uppercase `BKASH` / `ROCKET`.

Owner-reported real tests (status updated 2026-10-04; report date, not the transfer date):
- bKash: 320 BDT delivered to a public donation wallet. VERIFIED / DELIVERED, supported by Remitly
  In progress and Delivered emails.
- Rocket: 320 BDT sent from Poland to Bangladesh through Remitly. VERIFIED / DELIVERED: final
  Remitly status Delivered and receipt in the recipient account confirmed. The tested flow works end-to-end.
- Nagad: NOT SUPPORTED / EXCLUDED. Remitly requires a transfer-purpose classification for Nagad, and the available
  classifications do not accurately represent a tournament prize (Family support, Savings, Payment
  for service). Never use a false purpose. This explanation is internal, not player-facing copy.

Both bKash and Rocket have been verified with real successful Remitly transfers reaching final status
Delivered. The earlier unsuccessful Rocket test used an organization-style recipient name and was
paused. The successful Rocket test used the recipient's personal name matching the Rocket account.
Remitly requires accurate recipient identity details, supporting the existing separate `firstName`,
`lastName`, `walletProvider` and `walletNumber` fields. No real recipient name, number or reference
is recorded in this document.
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
messages are operational free text: operators must avoid copying recipient data into them. New audit
entries do not duplicate that text. The daily retention job now manages private recipient expiry and minimized payment records.

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
before a winner submits recipient details. Bangladesh-only privacy routing is prepared as of 2026-10-03. Publish the reviewed policy before releasing that build; Hosting is not deployed by this task.

## Retention and support

Policy version 1 uses `completed` and `cancelled` as terminal
statuses. The authoritative anchor is `statusUpdatedAt` when terminal retention is first recorded.
`sent` and `action_required` do not start retention. Missing terminal dates fail closed: no deletion.

`payments/{id}.retention` contains `policyVersion`, `terminalAt`, `rawExpiresAt`, `auditExpiresAt`,
and, after raw deletion, `rawDeletedAt`. Raw expiry is exactly 180 UTC days after terminal time.
Audit expiry is five calendar years later at the same UTC time; February 29 maps to February 28
when the anniversary year is not leap. Retries, account deletion and hold releases do not reset dates.

The administrator callable and local CLI now write retention deadlines atomically with terminal
Bangladesh status transitions. `onPaymentStatusChanged` remains a fallback for other trusted writes. Daily `cleanupBangladeshPayouts` (03:00 UTC, in the update-payment-status
codebase) also initializes missing metadata and processes overdue records. It scans payments in
100-document pages without requiring a composite index. Market selection uses the tournament's
`visibleInFlavours` containing `bangladesh`; once enrolled, `retention.policyVersion == 1` keeps the
record in scope even if tournament metadata disappears. Missing tournament data before enrollment
cannot establish Bangladesh scope and is skipped. This task does not migrate live data.

At 180 days the job deletes `private/recipient` and private transfer-attempt records and replaces the parent with an
allowlisted minimal record: userId, tournamentId, amount, currency, rank, status, lifecycle timestamps,
recipient schema version, retention metadata, wallet provider, safe transfer provider/reference and
sent/completed dates. Names, full wallet numbers, legacy recipientInfo, admin notes and issue text
are not retained in that minimized parent. The wallet provider is snapshotted before raw deletion.
Known recipient names/numbers and wallet-like long numbers in free-text transfer metadata are
excluded rather than copied into the minimized record. Operators must enter actual provider/reference
identifiers only. No full recipient details are copied to a separate audit collection.

At five years, delete the parent, any remaining private recipient document, and **only** new payment
`statusHistory` entries marked `retentionPolicyVersion: 1`. Up to 300 history entries and 100 private transfer-attempt records are removed per
transaction/run before final parent deletion, so large histories can require multiple daily runs.
**Existing unmarked statusHistory is never updated, migrated or deleted.** It may remain as a
subcollection under a missing parent and can contain legacy personal information. Support records,
notification bookkeeping, other user/game data, backups/exports and other-environment copies are not
covered by this job. A separate approved legacy/support/copy cleanup remains necessary.

### Transfer attempts and support completion (2026-10-05)

Each new `sent` transition from the CLI or callable creates
`payments/{id}/private/transfer-{historyId}` with `recordType: transfer_attempt`, an attempt ID,
provider, reference and sent timestamp. Retries in the same transaction do not duplicate it.
`transfer.attemptId` and subsequent history events link to that attempt; a later send does not
replace the earlier private record. Full attempt references are raw data subject to the same
180-day expiry and holds, paginated at 100 per cleanup pass. Long-term history stores only
attempt IDs, not names, wallet numbers or free-text references. Previously overwritten references
cannot be recovered and are not invented or backfilled.

On `completed`, the same transaction resolves existing active `validation_rejected` and
`payout_method_unavailable` tickets, recording `resolvedAt`, `resolutionCode: payment_completed`
and a system history event linked to the payment history. It does not auto-resolve
`payment_not_received`/`other` tickets, cancel-related complaints, or tickets while a payment
retention hold is active. New complaints raised after completion remain open for review.
Support context reads the wallet provider from `private/recipient` (or its minimized parent
snapshot), and derives BD market from the server-side tournament flavour when absent on the payment.

These lifecycle refinements are local changes pending deployment; running 540/550 from remote
`dev` does not publish uncommitted edits. Deploy 540 and 580 after committing/pushing them.
Existing completed payments and tickets are not retroactively resolved by this change.

### Holds and concurrent support handling

Any linked ticket not `resolved` or `closed` blocks both raw and audit deletion. Independently,
`payments/{id}.retentionHold.active == true` blocks deletion. Holds use reason codes `dispute` or
`legal_obligation`, with actor and timestamp, not free-text recipient data. There is no automatic
legal-hold expiry; an administrator must review and release it. After all blockers end, cleanup uses
the original dates and removes overdue data on the next successful run. Holds cannot restore data
already erased. Failed runs retry on subsequent scheduled runs; expiry is not instantaneous.

Admin-only callable: `setPayoutRetentionHold({paymentId, active, reasonCode})`.
Local operator commands (not executed by this task):

```powershell
node tools/update-payment-status/retention-hold.js dev PAYMENT_ID hold legal_obligation
node tools/update-payment-status/retention-hold.js dev PAYMENT_ID hold dispute
node tools/update-payment-status/retention-hold.js dev PAYMENT_ID release
```

Client rules already prohibit changing these parent fields and deleting private data; no rule change
is needed. Admin SDK credentials remain privileged. Cleanup reads the payment, recipient and support
query in a transaction. Support creation/reopening reads and touches the parent in its transaction,
so it serializes with cleanup; it cannot create/reopen a ticket for a deleted payment.

New payment history no longer copies administrator free text, provider/reference text or prior issue
messages. New support history stores message IDs rather than message copies. Operational support
messages and root previews still contain user/admin text: do not put recipient names/numbers there.
Old historical content is unchanged. Account removal deletes Auth and minimizes the profile to a deleted-user tombstone under the original UID (see ACCOUNT_DELETION.md);
payout retention runs independently and does not imply complete account-data erasure.

Deploy the updated update-payment-status codebase (including the scheduled job and hold callable),
support-tickets, and on-tournament-complete before relying on this policy. Cloud Build's 540 recipe
now copies retention.js. Local support/update-payment scripts must also use this code revision.
The previously committed code was deployed to DEV through 580, 590 and 540/550. The lifecycle
refinements above still require a new deployment. No historical purge or data migration is performed.

The 180-day/five-year durations are the owner's operational policy, not an assertion of a statutory
five-year requirement for full wallet numbers. Legal applicability, register scope and exceptions
remain subject to the source review in BANGLADESH_CONTENT_REVIEW.md.

## Verification

- Android validator tests: both providers, required first/last names, number and provider, normalization,
  unsupported/Nagad rejection, Unicode names and length bounds.
- Local Firestore emulator tests: required fields, ownership/read isolation, provider allowlist even
  under legacy rules, regulation constraints, batch atomicity, immutable submitted state and legacy-write rejection.
- Backend tests: normal lifecycle and correction, existing audit preservation and recipient-data exclusion from new events.
- Regulation/tournament tools: accepted provider metadata and rejected retired providers.
- Build and focused test both `_devGlobalDebug` and `_devBangladeshDebug`; manually test the form and
  real operator workflow before production. Rocket delivery has separately been verified by the owner
  with a successful real Remitly transfer; automated tests are not evidence of transfer delivery.

### Local verification results — 2026-10-02

- `_devGlobalDebug` and `_devBangladeshDebug` assemble: PASS.
- BangladeshPayoutAccountValidatorTest: 5/5 PASS in each variant.
- Node eligibility/payment/support/regulation/tournament/CLI suite: 40/40 PASS.
- Local Firestore rules emulator: 10/10 PASS.
- Full Global unit run (invoked before correcting the task-specific filter): 320 tests, 49 failures
  outside the payout validator, including resource/configuration and unrelated UI tests. The full
  suite is not green; those failures were not addressed by this payout change.
- No on-device form test, live database migration, new remittance or deployment was performed.


### Retention verification — 2026-10-04

- Retention/payment-workflow/admin CLI/support unit tests: 25/25 PASS.
- Firestore emulator rules and retention integration: 12/12 PASS. The integration exercises actual
  Admin SDK transactions, daily sweep queries, unresolved-ticket and legal holds, raw erasure,
  five-year cleanup, preserved legacy history and refusal to reopen a deleted payment's ticket.
- Tests cover exact expiry boundaries, leap-year anniversary, cancellation vs nonterminal status,
  Global exclusion, retry stability, missing dates, large new-history pages, and exclusion of names
  and full wallet numbers from minimized records and newly generated admin history.
- Winner cannot set retention deadlines or release a hold through Firestore rules.
- Changed JavaScript syntax and git diff whitespace checks: PASS. No deployment/live cleanup performed.
