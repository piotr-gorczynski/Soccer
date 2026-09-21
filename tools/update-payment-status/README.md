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
