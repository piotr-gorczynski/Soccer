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
`details` preserves the original supplied `issueCode`, `userMessage`, `notes`,
`provider`, and `providerReference`, including whitespace and newlines. Subsequent
commands do not overwrite these entries. `--dry-run` does not write history.
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
