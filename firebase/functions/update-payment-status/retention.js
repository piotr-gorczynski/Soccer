'use strict';

const TERMINAL = new Set(['completed', 'cancelled']);
const DAY = 86400000;
function dates(terminalAt) {
  if (!(terminalAt instanceof Date) || !Number.isFinite(terminalAt.getTime())) throw new Error('Invalid terminal date.');
  const raw = new Date(terminalAt.getTime() + 180 * DAY);
  const audit = new Date(terminalAt);
  const month = audit.getUTCMonth();
  audit.setUTCFullYear(audit.getUTCFullYear() + 5);
  // February 29 expires on February 28 in a non-leap anniversary year.
  if (audit.getUTCMonth() !== month) audit.setUTCDate(0);
  return { raw, audit };
}
function minimized(payment, recipient = {}) {
  // Explicit allowlist: never spread source data, free text, or recipient fields.
  const result = {};
  for (const field of ['userId', 'tournamentId', 'amount', 'currency', 'rank', 'status',
    'createdAt', 'statusUpdatedAt', 'updatedAt', 'recipientDetailsVersion', 'retention']) {
    if (payment[field] !== undefined) result[field] = payment[field];
  }
  const provider = recipient.walletProvider || payment.walletProvider;
  if (['BKASH', 'ROCKET'].includes(provider)) result.walletProvider = provider;
  const transfer = payment.transfer || {};
  result.transfer = {};
  // References are entered by admins; reject accidentally embedded recipient information.
  for (const field of ['provider', 'providerReference']) {
    const value = transfer[field];
    if (typeof value !== 'string') continue;
    const hasPrivateData = [recipient.firstName, recipient.lastName, recipient.walletNumber]
      .some(secret => typeof secret === 'string' && secret && value.toLowerCase().includes(secret.toLowerCase()));
    if (!hasPrivateData && !/\d{11,}/.test(value.replace(/[^0-9]/g, ''))) result.transfer[field] = value;
  }
  for (const field of ['sentAt', 'completedAt', 'attemptId']) {
    if (transfer[field]) result.transfer[field] = transfer[field];
  }
  return result;
}

async function processPayment(db, ref, now, Timestamp) {
  return db.runTransaction(async tx => {
    const snap = await tx.get(ref);
    if (!snap.exists) return 'missing';
    const payment = snap.data();
    if (!TERMINAL.has(payment.status)) return 'active';
    const tournament = payment.tournamentId
      ? await tx.get(db.collection('tournaments').doc(payment.tournamentId)) : null;
    const flavours = tournament?.exists ? tournament.get('visibleInFlavours') : [];
    const isBd = payment.retention?.policyVersion === 1 ||
      (Array.isArray(flavours) && flavours.includes('bangladesh'));
    if (!isBd) return 'other-market';
    const recipientRef = ref.collection('private').doc('recipient');
    const recipient = await tx.get(recipientRef);
    const tickets = await tx.get(db.collection('supportTickets').where('paymentId', '==', ref.id));
    const held = payment.retentionHold?.active === true || tickets.docs.some(t =>
      !['resolved', 'closed'].includes(t.get('status')));
    let retention = payment.retention;
    if (!retention) {
      const terminalAt = payment.statusUpdatedAt;
      if (!terminalAt || typeof terminalAt.toDate !== 'function') return 'missing-terminal-date';
      const expiry = dates(terminalAt.toDate());
      retention = { policyVersion: 1, terminalAt,
        rawExpiresAt: Timestamp.fromDate(expiry.raw), auditExpiresAt: Timestamp.fromDate(expiry.audit) };
    }
    if (retention.policyVersion !== 1 || typeof retention.rawExpiresAt?.toMillis !== 'function'
        || typeof retention.auditExpiresAt?.toMillis !== 'function') return 'invalid-retention-metadata';
    const rawDue = now.getTime() >= retention.rawExpiresAt.toMillis();
    const auditDue = now.getTime() >= retention.auditExpiresAt.toMillis();
    const attempts = rawDue && !held
      ? await tx.get(ref.collection('private').where('recordType', '==', 'transfer_attempt').limit(100)) : null;
    // Query only new-policy history. Legacy statusHistory is never changed or deleted.
    const history = auditDue && !held
      ? await tx.get(ref.collection('statusHistory').where('retentionPolicyVersion', '==', 1).limit(300)) : null;
    if (held) {
      if (!payment.retention) tx.update(ref, { retention });
      return 'held';
    }
    if (attempts) for (const attempt of attempts.docs) tx.delete(attempt.ref);
    if (auditDue) {
      for (const doc of history.docs) tx.delete(doc.ref);
      tx.delete(recipientRef);
      if (history.size === 300 || attempts.size === 100) {
        const reduced = minimized({ ...payment, retention }, recipient.exists ? recipient.data() : {});
        reduced.retention.rawDeletedAt = retention.rawDeletedAt || Timestamp.fromDate(now);
        if (payment.retentionHold) reduced.retentionHold = payment.retentionHold;
        tx.set(ref, reduced);
        return 'history-page';
      }
      tx.delete(ref);
      return 'audit-deleted';
    }
    if (rawDue) {
      const reduced = minimized({ ...payment, retention }, recipient.exists ? recipient.data() : {});
      reduced.retention.rawDeletedAt = retention.rawDeletedAt || Timestamp.fromDate(now);
      if (payment.retentionHold) reduced.retentionHold = payment.retentionHold;
      tx.set(ref, reduced);
      tx.delete(recipientRef);
      return 'raw-deleted';
    }
    if (!payment.retention) {
      const provider = recipient.exists ? recipient.get('walletProvider') : null;
      tx.update(ref, { retention, ...(['BKASH', 'ROCKET'].includes(provider) ? { walletProvider: provider } : {}) });
    }
    return 'retained';
  });
}

async function sweep(db, now, Timestamp) {
  let cursor;
  const counts = {};
  do {
    let query = db.collection('payments').orderBy('__name__').limit(100);
    if (cursor) query = query.startAfter(cursor);
    const page = await query.get();
    if (page.empty) break;
    for (const doc of page.docs) {
      const outcome = await processPayment(db, doc.ref, now, Timestamp);
      counts[outcome] = (counts[outcome] || 0) + 1;
    }
    cursor = page.docs[page.docs.length - 1];
  } while (cursor);
  return counts;
}

async function setHold(db, paymentId, active, reasonCode, actor, Timestamp) {
  if (typeof active !== 'boolean' || !['dispute', 'legal_obligation', 'released'].includes(reasonCode)
      || (active && reasonCode === 'released') || (!active && reasonCode !== 'released')) {
    throw new Error('Use active=true with dispute/legal_obligation or active=false with released.');
  }
  const ref = db.collection('payments').doc(paymentId);
  return db.runTransaction(async tx => {
    const snap = await tx.get(ref);
    if (!snap.exists) throw new Error('Payment not found.');
    tx.update(ref, { retentionHold: { active, reasonCode, updatedAt: Timestamp.now(), updatedBy: actor } });
  });
}
module.exports = { dates, minimized, processPayment, sweep, setHold };
