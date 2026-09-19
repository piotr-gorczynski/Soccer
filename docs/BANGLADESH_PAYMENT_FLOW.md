# Bangladesh Prize Payment Flow

**Status:** Proposed target design  
**Last updated:** 2026-09-19

## Purpose

This document defines the business and technical workflow for manually paying Bangladesh tournament
prizes. Winners provide a supported mobile-wallet account in the app, an administrator creates and
tracks the transfer through a remittance provider such as Remitly, and the app communicates the
current state to the winner.

The payment record is the source of truth for the prize workflow. A saved payout account is not proof
that a transfer has started, and a submitted transfer is not proof that the recipient has received the
money.

## Design principles

1. Show the winner what action, if any, is required.
2. Keep recipient-data collection separate from transfer execution.
3. Mark a payment as completed only after the provider confirms delivery.
4. Allow incorrect or rejected recipient details to be corrected without losing audit history.
5. Keep administrative notes private and expose only safe, actionable messages to the winner.
6. Do not call bKash, Nagad, or Rocket details “bank details”; use “payment details” or “payout details”.

Remitly distinguishes transfers that are in progress from transfers that have been delivered. Its
public guidance also describes completion as the point at which funds have been deposited into the
recipient's account or collected by the recipient. The application therefore must not treat transfer
creation as successful delivery.

- [Remitly transfer tracking](https://www.remitly.com/us/en/home/send_to/mexico)
- [Remitly error resolution and cancellation disclosure](https://www.remitly.com/pdf/en/us/476c6e48-9362-4046-98ab-f80969329243)

## Status model

The technical status must be stored in English as a stable machine-readable value. The app translates
the user-facing title and description.

| Technical status | User-facing title | Meaning | Winner action |
|---|---|---|---|
| `awaiting_details` | Payment pending | The prize exists, but valid payout details have not been submitted. | Select a supported method and enter the account number. |
| `ready_for_processing` | Payment details received | Valid details have been saved and the payout is waiting for administrative processing. | None. |
| `processing` | Payment in progress | The administrator has started creating or reviewing the transfer with the provider. | None. |
| `sent` | Money sent to recipient | The provider accepted the transfer and it is on its way to the recipient. | Wait for delivery. |
| `completed` | Payment delivered | The provider confirms that the funds were delivered to the recipient account. | None. This is a terminal state. |
| `action_required` | Action required | Processing or delivery failed because the payout details must be corrected. | Correct and resubmit the details. |
| `cancelled` | Payment cancelled | The payout was cancelled by an administrator for an exceptional, documented reason. | Contact support if the displayed message requests it. |

“Payment pending” is intentionally only a user-facing phrase. The backend uses the more precise
`awaiting_details` status so that an operator can distinguish missing details from details that have
already been submitted.

## Allowed transitions

```text
awaiting_details
        | winner submits valid details
        v
ready_for_processing
        | administrator starts the transfer
        v
processing
        | provider accepts/sends the transfer
        v
sent
        | provider confirms delivery
        v
completed
```

Correction flow:

```text
ready_for_processing | processing | sent
        | details rejected or recipient cannot be paid
        v
action_required
        | winner corrects and resubmits details
        v
ready_for_processing
```

Administrative cancellation may be allowed from `awaiting_details`, `ready_for_processing`,
`processing`, or `action_required`. `completed` is terminal and must not be reopened through the
normal admin function. Any exceptional correction after completion must be recorded as a separate
administrative adjustment rather than rewriting payment history.

The backend must reject every transition not listed above.

## Winner experience

### Status presentation

The payout card should display a prominent status row directly below the prize amount. It should
contain a short title and an explanatory sentence, for example:

```text
Status: Payment pending
Enter your payment details to receive the prize.
```

After submission:

```text
Status: Payment details received
Your prize is waiting to be processed.
```

The existing `Payment details saved` text should be replaced by this status component. A short toast
may still confirm a successful save, but the persistent status must come from the payment document.

### Editing rules

- `awaiting_details`: method and account number are editable; the save button is visible.
- `ready_for_processing`, `processing`, and `sent`: details are read-only and the save button is hidden.
- `action_required`: details become editable again and the save button label changes to
  `Resubmit payment details`.
- `completed` and `cancelled`: details are read-only.

After submission, the account number should preferably be masked in the UI, leaving only enough
digits for the winner to identify it. The complete value remains available only where required for
authorized payout processing.

### Incorrect details

When a provider rejects the recipient data, the administrator changes the payment to
`action_required` and selects a safe reason code. The app displays a localized, actionable message,
for example:

```text
Action required
We could not process this payment because the recipient account details were rejected.
Check the account number and submit the details again.
```

The user must never see raw provider errors, internal notes, fraud indicators, or other sensitive
administrative information.

## Administrative workflow

1. Filter payments with status `ready_for_processing`.
2. Verify the tournament, winner, amount, currency, payout method, and recipient details.
3. Set the payment to `processing` immediately before beginning the provider workflow.
4. Create the transfer in Remitly or another approved provider.
5. Store the provider name and provider reference, then set the payment to `sent` after the provider
   accepts the transfer.
6. Set the payment to `completed` only after the provider confirms delivery.
7. If the recipient details are rejected, set `action_required`, choose an issue code, and provide a
   safe message for the winner.

The administrator must not edit the payment document directly in Firestore during normal operation.
Status changes should go through an authenticated admin function so transitions, timestamps, and the
audit record remain consistent.

## Recommended Firestore model

```javascript
// payments/{paymentId}
{
  userId: "winner-user-id",
  tournamentId: "tournament-id",
  rank: 1,
  amount: 1000,
  currency: "BDT",

  status: "ready_for_processing",
  statusUpdatedAt: Timestamp,

  recipientInfo: {
    method: "bkash",                 // Loaded from the regulation
    accountNumber: "01XXXXXXXXX",    // Normalized and validated
    submittedAt: Timestamp,
    updatedAt: Timestamp
  },

  transfer: {
    provider: "remitly",
    providerReference: "provider-reference",
    initiatedAt: Timestamp,
    sentAt: Timestamp,
    completedAt: Timestamp,
    transferFee: 0.0,
    feeCurrency: "PLN",
    exchangeRate: 0.0
  },

  issue: {
    code: "invalid_recipient_account",
    userMessage: "Check the account number and submit the details again.",
    createdAt: Timestamp,
    resolvedAt: Timestamp
  },

  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

Fields that do not apply to the current status should be absent instead of being written as empty
strings. Provider-specific values belong under `transfer`; they must not be mixed with the payout
method selected by the winner.

## Audit history

Every accepted transition should create a server-managed event:

```javascript
// payments/{paymentId}/statusHistory/{eventId}
{
  from: "processing",
  to: "action_required",
  changedAt: Timestamp,
  changedBy: "admin-user-id",       // or "system" / winner user ID
  source: "admin_function",
  reasonCode: "invalid_recipient_account"
}
```

A subcollection is preferred over an ever-growing array in the payment document. Clients may read
the safe status state they need, but only trusted server code may write history events.

Internal administrative notes should be stored separately from user-visible messages and protected
from winner access by Firestore rules.

## Backend responsibilities

### Creating a payment

`onTournamentComplete` should create the payment with `status: "awaiting_details"` and no
`recipientInfo`.

### Submitting payout details

The current client directly updates `recipientInfo` while the payment remains `pending`. The target
implementation should use a callable function such as `submitPayoutDetails` that atomically:

1. verifies that the authenticated caller owns the payment;
2. permits submission only from `awaiting_details` or `action_required`;
3. verifies the payout method against the tournament regulation;
4. normalizes and validates the account number;
5. writes `recipientInfo` and its server timestamps;
6. clears the resolved user-facing issue;
7. changes the status to `ready_for_processing`;
8. creates the status-history event.

This prevents a client write from saving details without advancing the workflow or advancing the
status without valid details.

### Administrative status updates

`updatePaymentStatus` should be extended to support the new statuses and transition table. Provider
reference data should be required when moving to `sent`. The function should set all relevant server
timestamps and create a history event in the same transaction or batch.

### Notifications

Send an FCM notification when:

- payout details are required after winning;
- payment changes to `sent`;
- payment changes to `completed`;
- payment changes to `action_required`.

Do not notify for purely internal transitions unless the user benefits from the information.

## Existing development data

Backward compatibility with the old `pending` status is not required. Existing development records
must be updated once before the new workflow is enabled:

- a record without `recipientInfo` becomes `awaiting_details`;
- a record with valid `recipientInfo` becomes `ready_for_processing`.

After this one-time data update, neither the mobile application nor the backend should interpret or
create `pending`. Encountering an unknown or retired status must produce a controlled error and must
not silently infer a workflow state.

## Security and privacy requirements

- A winner may read only their own payment record.
- A winner may submit details only through the guarded submission path.
- Only an administrator may start, send, complete, cancel, or reject a payout.
- Account numbers must not appear in logs, analytics events, crash reports, or notifications.
- Provider references and full recipient details must not be exposed to other tournament users.
- Status history and timestamps must be generated by trusted server code.
- User-facing issue messages must not expose internal risk or provider diagnostics.

## Implementation sequence

1. Add the new status constants and transition tests to the backend.
2. Implement `submitPayoutDetails` and update Firestore rules.
3. Extend `updatePaymentStatus` and add status-history writes.
4. Add localized status titles, descriptions, and correction messages.
5. Replace `Payment details saved` with the persistent status component on the payout card.
6. Add read-only, editable, and action-required UI states.
7. Add FCM notifications for `sent`, `completed`, and `action_required`.
8. Add an admin workflow for pending payout operations.
9. Update the existing development payment records and verify that no `pending` record remains.
10. Test the normal, rejected-details, retry, and cancellation paths on `dev`.
