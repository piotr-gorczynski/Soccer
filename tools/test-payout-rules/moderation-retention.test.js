const { test, after } = require('node:test');
const assert = require('node:assert/strict');
const { initializeApp, deleteApp } = require('firebase-admin/app');
const { getFirestore, Timestamp, FieldValue } = require('firebase-admin/firestore');
const { legalExpiry, reportExpiry, cleanReport, cleanLegal, setModerationHold, sweep } =
  require('../../firebase/functions/support-tickets/retention');
if (!process.env.FIRESTORE_EMULATOR_HOST) throw Error('Local emulator required');
const app = initializeApp({ projectId: 'demo-moderation-retention' }, 'moderation-retention');
const db = getFirestore(app);
const ts = value => Timestamp.fromDate(new Date(value));
const now = new Date('2030-07-01T00:00:00Z');
after(() => deleteApp(app));

test('expiry uses 180 exact days and five UTC calendar years, including leap day', () => {
  assert.equal(reportExpiry({ type: 'moderation', status: 'resolved', resolvedAt: ts('2030-01-02T00:00:00Z') }).toISOString(), now.toISOString());
  assert.equal(legalExpiry(ts('2024-02-29T12:30:00Z')).toISOString(), '2029-02-28T12:30:00.000Z');
  assert.equal(legalExpiry(null), null);
});
test('open/missing-date/future reports and payout tickets survive; due report and nested history deleted', async () => {
  for (const [id, data, expected] of [
    ['open', { status: 'open', resolvedAt: ts('2000-01-01Z') }, 'open'],
    ['missing', { status: 'closed' }, 'missing-resolution-date'],
    ['future', { status: 'resolved', resolvedAt: ts('2030-06-30Z') }, 'retained'],
    ['payout', { type: 'payout', status: 'resolved', resolvedAt: ts('2000-01-01Z') }, 'not-moderation'],
  ]) {
    const ref = db.doc('supportTickets/' + id);
    await ref.set({ type: 'moderation', ...data });
    const before = await ref.get();
    assert.equal(await cleanReport(db, ref, now, FieldValue), expected);
    assert.ok((await ref.get()).updateTime.isEqual(before.updateTime));
  }
  for (const status of ['resolved', 'closed']) {
    const ref = db.doc('supportTickets/due-' + status);
    await ref.set({ type: 'moderation', status, statusUpdatedAt: ts('2030-01-02T00:00:00Z') });
    const history = ref.collection('statusHistory').doc('event');
    const nested = history.collection('nested').doc('entry');
    const orphan = ref.collection('messages').doc('missing').collection('nested').doc('entry');
    await history.set({ test: true }); await nested.set({ test: true }); await orphan.set({ test: true });
    assert.equal(await cleanReport(db, ref, now, FieldValue), 'report-deleted');
    for (const doc of [ref, history, nested, orphan]) assert.equal((await doc.get()).exists, false);
    assert.equal(await cleanReport(db, ref, now, FieldValue), 'missing');
  }
});
test('hold blocks cleanup; releasing keeps deadline; partial deletion is retryable and cannot accept new hold', async () => {
  const ref = db.doc('supportTickets/hold');
  const resolvedAt = ts('2029-01-01Z');
  await ref.set({ type: 'moderation', status: 'resolved', resolvedAt });
  await setModerationHold(db, ref, true, 'TEST security dispute', 'TEST admin', FieldValue);
  assert.equal(await cleanReport(db, ref, now, FieldValue), 'held');
  await setModerationHold(db, ref, false, 'TEST resolved', 'TEST admin', FieldValue);
  assert.ok((await ref.get()).get('resolvedAt').isEqual(resolvedAt));
  const failingDb = { runTransaction: cb => db.runTransaction(cb), recursiveDelete: async () => { throw Error('TEST outage'); } };
  await assert.rejects(cleanReport(failingDb, ref, now, FieldValue), /outage/);
  assert.equal((await ref.get()).exists, true);
  await assert.rejects(setModerationHold(db, ref, true, 'TEST late', 'admin', FieldValue), /already started/);
  assert.equal(await cleanReport(db, ref, now, FieldValue), 'report-deleted');
});
test('legal evidence retained immutable until deletion anniversary; deletes only acceptances and descendants', async () => {
  const ref = db.doc('users/deleted');
  await ref.set({ accountDeleted: true, accountDeletedAt: ts('2025-07-01T00:00:00Z'), nickname: '(Account removed)' });
  const evidence = ref.collection('legalAcceptances').doc('terms');
  await evidence.set({ version: 'TEST', acceptedAt: ts('2020-01-01Z') });
  await evidence.collection('nested').doc('test').set({ test: true });
  await db.doc('matches/historical').set({ player0: ref.id });
  const before = await evidence.get(), root = await ref.get();
  assert.equal(await cleanLegal(db, ref, new Date(now.getTime() - 1)), 'retained');
  assert.ok((await evidence.get()).updateTime.isEqual(before.updateTime));
  assert.equal(await cleanLegal(db, ref, now), 'legal-deleted');
  assert.equal((await evidence.get()).exists, false);
  assert.equal((await evidence.collection('nested').doc('test').get()).exists, false);
  assert.ok((await ref.get()).updateTime.isEqual(root.updateTime));
  assert.equal((await db.doc('matches/historical').get()).get('player0'), ref.id);
  assert.equal(await cleanLegal(db, ref, now), 'already-legal-deleted');
  for (const [id, data, expected] of [
    ['active', { accountDeleted: false, accountDeletedAt: ts('2000-01-01Z') }, 'active-account'],
    ['no-date', { accountDeleted: true }, 'missing-deletion-date'],
  ]) {
    const user = db.doc('users/' + id); await user.set(data);
    await user.collection('legalAcceptances').doc('terms').set({ test: true });
    assert.equal(await cleanLegal(db, user, now), expected);
    assert.equal((await user.collection('legalAcceptances').doc('terms').get()).exists, true);
  }
  const summary = await sweep(db, now, FieldValue);
  assert.equal(summary['already-legal-deleted'], 1);
});
