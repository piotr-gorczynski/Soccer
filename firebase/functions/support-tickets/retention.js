'use strict';

const DAY = 86400000;
const terminal = status => ['resolved', 'closed'].includes(status);
function timestampDate(value) {
  if (typeof value?.toDate !== 'function') return null;
  const date = value.toDate();
  return Number.isFinite(date.getTime()) ? date : null;
}
function legalExpiry(deletedAt) {
  const date = timestampDate(deletedAt);
  if (!date) return null;
  const month = date.getUTCMonth();
  date.setUTCFullYear(date.getUTCFullYear() + 5);
  if (date.getUTCMonth() !== month) date.setUTCDate(0);
  return date;
}
function reportExpiry(data) {
  if (data.type !== 'moderation' || !terminal(data.status)) return null;
  const date = timestampDate(data.status === 'closed' ? data.closedAt : data.resolvedAt)
    || timestampDate(data.statusUpdatedAt);
  return date ? new Date(date.getTime() + 180 * DAY) : null;
}

// Transactional claim serializes cleanup with administrator holds. Keep the parent
// until all descendants are gone so a failed recursive cleanup can safely retry.
async function cleanReport(db, ref, now, FieldValue) {
  const result = await db.runTransaction(async tx => {
    const snap = await tx.get(ref);
    if (!snap.exists) return 'missing';
    const data = snap.data();
    if (data.type !== 'moderation') return 'not-moderation';
    if (data.retentionHold?.active === true) return 'held';
    if (!terminal(data.status)) return 'open';
    const expiry = reportExpiry(data);
    if (!expiry) return 'missing-resolution-date';
    if (expiry > now) return 'retained';
    if (!data.retentionDeletionStartedAt) {
      tx.update(ref, { retentionDeletionStartedAt: FieldValue.serverTimestamp() });
    }
    return 'delete';
  });
  if (result !== 'delete') return result;
  for (const collection of await ref.listCollections()) await db.recursiveDelete(collection);
  await ref.delete();
  return 'report-deleted';
}

async function setModerationHold(db, ref, active, reason, actor, FieldValue) {
  if (typeof active !== 'boolean' || typeof reason !== 'string' || !reason.trim()
      || reason.length > 1000) throw Error('Hold/release requires a reason (1–1000 characters).');
  return db.runTransaction(async tx => {
    const snap = await tx.get(ref);
    if (!snap.exists || snap.get('type') !== 'moderation') throw Error('Moderation report not found.');
    if (snap.get('retentionDeletionStartedAt')) throw Error('Deletion already started; hold cannot change.');
    tx.update(ref, { retentionHold: { active, reason: reason.trim(), changedBy: actor,
      changedAt: FieldValue.serverTimestamp() } });
    tx.set(ref.collection('statusHistory').doc(), { eventType: active ? 'retention_hold' : 'retention_hold_released',
      reason: reason.trim(), changedBy: actor, changedAt: FieldValue.serverTimestamp() });
  });
}

async function cleanLegal(db, ref, now) {
  const snap = await ref.get();
  if (!snap.exists || snap.get('accountDeleted') !== true) return 'active-account';
  const expiry = legalExpiry(snap.get('accountDeletedAt'));
  if (!expiry) return 'missing-deletion-date';
  if (expiry > now) return 'retained';
  // Includes any nested/orphaned history. Never edit acceptance evidence or the UID tombstone.
  const collection = ref.collection('legalAcceptances');
  if (!(await collection.listDocuments()).length) return 'already-legal-deleted';
  await db.recursiveDelete(collection);
  return 'legal-deleted';
}

async function sweep(db, now, FieldValue) {
  const summary = {};
  for (const [query, process] of [
    [db.collection('supportTickets').where('type', '==', 'moderation'), ref => cleanReport(db, ref, now, FieldValue)],
    [db.collection('users').where('accountDeleted', '==', true), ref => cleanLegal(db, ref, now)],
  ]) {
    let last;
    for (;;) {
      let pageQuery = query.orderBy('__name__').limit(100);
      if (last) pageQuery = pageQuery.startAfter(last);
      const page = await pageQuery.get();
      if (page.empty) break;
      for (const doc of page.docs) {
        const result = await process(doc.ref);
        summary[result] = (summary[result] || 0) + 1;
      }
      last = page.docs[page.docs.length - 1];
    }
  }
  return summary;
}
module.exports = { legalExpiry, reportExpiry, cleanReport, cleanLegal, setModerationHold, sweep };
