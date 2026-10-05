'use strict';

const VALID_STATUSES = new Set([
  'awaiting_details',
  'ready_for_processing',
  'processing',
  'sent',
  'completed',
  'action_required',
  'cancelled',
]);

const ALLOWED_TRANSITIONS = {
  awaiting_details: new Set(['cancelled']),
  ready_for_processing: new Set(['processing', 'action_required', 'cancelled']),
  processing: new Set(['sent', 'action_required', 'cancelled']),
  sent: new Set(['completed', 'action_required']),
  completed: new Set(),
  action_required: new Set(['cancelled']),
  cancelled: new Set(),
};

const NOTIFIABLE_STATUSES = new Set([
  'processing',
  'sent',
  'completed',
  'action_required',
  'cancelled',
]);

function assertTransition(currentStatus, targetStatus) {
  if (!VALID_STATUSES.has(targetStatus)) {
    throw new Error(`Unknown target payment status: ${targetStatus}.`);
  }
  const allowed = ALLOWED_TRANSITIONS[currentStatus];
  if (!allowed || !allowed.has(targetStatus)) {
    throw new Error(`Cannot transition payment from "${currentStatus}" to "${targetStatus}".`);
  }
}

function validateTransitionData(targetStatus, data = {}) {
  if (targetStatus === 'sent') {
    if (typeof data.provider !== 'string' || !data.provider.trim()) {
      throw new Error('`provider` is required when marking a payment as sent.');
    }
    if (typeof data.providerReference !== 'string' || !data.providerReference.trim()) {
      throw new Error('`providerReference` is required when marking a payment as sent.');
    }
  }
  if (targetStatus === 'action_required') {
    if (typeof data.issueCode !== 'string' || !data.issueCode.trim()) {
      throw new Error('`issueCode` is required when action is required.');
    }
    if (typeof data.userMessage !== 'string' || !data.userMessage.trim()) {
      throw new Error('`userMessage` is required when action is required.');
    }
  }
}

// Historical events contain metadata only. Free text may contain recipient PII.
function buildAdminHistory(from, to, data, actor, source, changedAt) {
  const details = {};
  if (typeof data.clearIssue === 'boolean') details.clearIssue = data.clearIssue;
  return {
    retentionPolicyVersion: 1,
    eventType: 'admin_status_changed', from, to, changedAt,
    changedBy: actor, actorType: 'admin', source, details,
  };
}

async function recordRecipientSubmission(db, paymentRef, before, after, eventId, FieldValue) {
  if (!['awaiting_details', 'action_required'].includes(before.status)
      || after.status !== 'ready_for_processing') return;
  const historyRef = paymentRef.collection('statusHistory').doc(`recipient-${eventId}`);
  // A retried trigger must not duplicate or rewrite an already recorded event.
  await db.runTransaction(async transaction => {
    if ((await transaction.get(historyRef)).exists) return;
    transaction.set(historyRef, {
      retentionPolicyVersion: 1,
      eventType: 'recipient_details_submitted',
      from: before.status, to: after.status,
      changedAt: after.statusUpdatedAt,
      recordedAt: FieldValue.serverTimestamp(),
      changedBy: after.userId, actorType: 'user',
      source: 'onPaymentStatusChanged',
      // Recipient fields never belong in newly generated audit events.
      recipientDetailsVersion: 2,
      hadPreviousIssue: Boolean(before.issue),
    });
  });
}

module.exports = {
  VALID_STATUSES,
  ALLOWED_TRANSITIONS,
  NOTIFIABLE_STATUSES,
  assertTransition,
  validateTransitionData,
  buildAdminHistory,
  recordRecipientSubmission,
};

// All lifecycle writes share the administrator's transaction (CLI and callable).
async function persistTransition(tx, db, ref, snap, status, data, historyRef, update, history, Timestamp) {
  const now = Timestamp.now();
  const payment = snap.data();
  const terminal = ['completed', 'cancelled'].includes(status);
  const tournament = terminal && payment.tournamentId
    ? await tx.get(db.collection('tournaments').doc(payment.tournamentId)) : null;
  const tickets = status === 'completed'
    ? await tx.get(db.collection('supportTickets').where('paymentId', '==', ref.id)) : { docs: [] };
  update.statusUpdatedAt = now;
  update.updatedAt = now;
  history.changedAt = now;
  if (status === 'sent') {
    update['transfer.sentAt'] = now;
    update['transfer.attemptId'] = historyRef.id;
    history.details = { ...history.details, attemptId: historyRef.id };
    // Raw references are not safe for long-term history: admins may enter PII.
    tx.set(ref.collection('private').doc('transfer-' + historyRef.id), {
      recordType: 'transfer_attempt', attemptId: historyRef.id,
      provider: data.provider.trim(), providerReference: data.providerReference.trim(), sentAt: now,
    });
  } else if (payment.transfer?.attemptId) {
    history.details = { ...history.details, attemptId: payment.transfer.attemptId };
  }
  if (status === 'completed') update['transfer.completedAt'] = now;
  if (terminal && (payment.retention?.policyVersion === 1 ||
      tournament?.data()?.visibleInFlavours?.includes('bangladesh'))) {
    const expiry = require('./retention').dates(now.toDate());
    update.retention = payment.retention || { policyVersion: 1, terminalAt: now,
      rawExpiresAt: Timestamp.fromDate(expiry.raw), auditExpiresAt: Timestamp.fromDate(expiry.audit) };
  }
  if (status === 'completed' && !payment.retentionHold?.active) {
    for (const ticket of tickets.docs) {
      if (!['open', 'waiting_for_user', 'in_progress'].includes(ticket.get('status')) ||
          !['validation_rejected', 'payout_method_unavailable'].includes(ticket.get('category'))) continue;
      tx.update(ticket.ref, { status: 'resolved', statusUpdatedAt: now, updatedAt: now,
        resolvedAt: now, resolutionCode: 'payment_completed' });
      tx.set(ticket.ref.collection('statusHistory').doc('payment-' + historyRef.id), {
        eventType: 'payment_completed', from: ticket.get('status'), to: 'resolved', changedAt: now,
        actorType: 'system', source: 'payment_transition', paymentHistoryId: historyRef.id,
      });
    }
  }
  tx.update(ref, update);
  tx.set(historyRef, history);
}
module.exports.persistTransition = persistTransition;
