# Update Payment Status Tool

Updates a prize payment through the controlled status-transition model and writes an audit event to
`payments/{paymentId}/statusHistory`. A deployed `onPaymentStatusChanged` trigger sends the winner an
FCM notification for user-visible status changes.

## Install

```bash
cd tools/update-payment-status
npm install
```

The tool uses `secrets/serviceAccountKey.<env>.json`.

## List incomplete payments

List every payment whose status is not `completed`:

```bash
node update-payment-status.js dev list
```

The output includes the payment ID, status, amount, currency, user ID, and tournament ID. Replace `dev`
with `test` or `prod` to inspect another environment.

## Preview and update

```bash
node update-payment-status.js dev PAYMENT_ID processing --dry-run
node update-payment-status.js dev PAYMENT_ID processing
```

Mark a provider transfer as sent:

```bash
node update-payment-status.js dev PAYMENT_ID sent \
  --provider remitly --reference REMITLY_REFERENCE
```

Request corrected winner details:

```bash
node update-payment-status.js dev PAYMENT_ID action_required \
  --issue-code invalid_recipient_account \
  --user-message "Check the account number and submit the details again."
```

Complete a delivered payment:

```bash
node update-payment-status.js dev PAYMENT_ID completed
```

The tool rejects unknown statuses and transitions that are not allowed by
`firebase/functions/update-payment-status/payment-workflow.js`.

## History

Each successful command atomically updates the payment and appends a document to
`payments/{paymentId}/statusHistory`. The entry includes `from`, `to`, server-side
`changedAt`, `actorType`, `changedBy` (the CLI service-account email), and `source`.
New history retains only metadata and an explicit `clearIssue` flag. It does not copy free-text
notes, messages, issue codes or provider/reference fields, which may contain personal information.
Existing history is unchanged. `--dry-run` does not write history.
The admin callable uses the same format, identifying the authenticated admin UID.

Payment creation records `payment_created`; the payment-status trigger records
each user's `recipient_details_submitted` event with old/new recipient details.
For submissions, `changedAt` is the original `statusUpdatedAt`, while `recordedAt`
is the trigger's write time. Trigger retries reuse an event ID to avoid duplicates.
Sort history by `changedAt`, not document ID or trigger arrival time.

These records are for trusted administration and are not client-writable/readable.
CLI history works immediately with the updated tool. For the app and server paths,
deploy Firestore rules, `update-payment-status`, `on-tournament-complete`, and
`support-tickets`, then install the updated app. Deploy rules before the app because
the new app also writes `updatedAt` when submitting recipient details. Older clients
remain supported by the rules and the submission-history trigger.

The changes record future actions only. Previously overwritten messages and missing
timestamps cannot be reconstructed reliably and are not backfilled.

## Recipient schema v2 (Bangladesh)

Current recipient details are stored at `payments/{paymentId}/private/recipient` with separate
`firstName`, `lastName`, `walletProvider` (`BKASH` or `ROCKET`) and `walletNumber` fields.
The list command intentionally omits sensitive recipient data. Use authorized Console/Admin SDK
access to the private document when processing manually in Remitly. Do not put recipient names or
numbers into `--notes` or user messages. All four recipient fields are required before processing; do not invent names from a nickname.
Bangladesh has not launched in production; old test schemas are not supported.

Real tests: bKash 320 BDT delivered; Rocket 320 BDT sent/in progress (delivery not confirmed).
Mark `completed` only on delivery evidence. See `docs/BANGLADESH_PAYMENT_FLOW.md` for rollout.


### Payout retention holds

For an unresolved dispute or legal obligation, suspend payout cleanup:

```powershell
node tools/update-payment-status/retention-hold.js dev PAYMENT_ID hold legal_obligation
node tools/update-payment-status/retention-hold.js dev PAYMENT_ID hold dispute
node tools/update-payment-status/retention-hold.js dev PAYMENT_ID release
```

Release does not restart the 180-day / five-year deadlines. Open support tickets independently block
cleanup. History now records status/actor/time metadata without copying free-text notes or references;
current operational payment fields still store supplied text until minimization.
See docs/BANGLADESH_PAYMENT_FLOW.md for deployment and legacy-history exclusions.
