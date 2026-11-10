'use strict';
const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, getDoc, setDoc, updateDoc, getDocs, query, collection, where } = require('firebase/firestore');
const { initializeApp, deleteApp } = require('firebase-admin/app');
const { getFirestore, FieldValue, Timestamp } = require('firebase-admin/firestore');
const { handlers } = require('../../firebase/functions/support-tickets/conversation');
const { updateTicket } = require('../support-tickets/support-tickets');
if (!process.env.FIRESTORE_EMULATOR_HOST) throw Error('Emulator required');
class HttpsError extends Error { constructor(code, message) { super(message); this.code = code; } }
let env, app, db, api, now;
const auth = uid => ({ auth: { uid } });
const owner = auth('owner');
const request = (extra = {}) => ({ ticketId: 'report', message: 'Please review this.',
  requestId: 'request_1234567890', ...extra });
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-support-conversations', firestore: {
    host: '127.0.0.1', port: 8188, rules: fs.readFileSync('../../firebase/firestore.rules', 'utf8'),
  } });
  app = initializeApp({ projectId: 'demo-support-conversations' }, 'support-conversations');
  db = getFirestore(app);
  api = handlers(db, FieldValue, HttpsError, () => now);
});
after(async () => { await env?.cleanup(); if (app) await deleteApp(app); });
beforeEach(async () => {
  await env.clearFirestore();
  now = Date.UTC(2026, 10, 10, 10);
  await db.doc('users/owner').set({ nickname: 'Owner' });
  await db.doc('users/target').set({ nickname: 'Target' });
  await db.doc('supportTickets/report').set({ userId: 'owner', reporterUid: 'owner', reportedUid: 'target',
    nickname: 'Target', type: 'moderation', reference: 'MOD-EXAMPLE', category: 'player_report',
    message: 'Original report', status: 'open', createdAt: Timestamp.fromMillis(now - 10000),
    updatedAt: Timestamp.fromMillis(now - 10000), retentionHold: { reason: 'Internal note', changedBy: 'admin@example.invalid' } });
});

test('anonymous authenticated owner can read; unauthenticated, other player and deleted accounts cannot', async () => {
  await assert.rejects(api.getSupportConversation({ ticketId: 'report' }, {}), { code: 'unauthenticated' });
  await assert.rejects(api.getSupportConversation({ ticketId: 'report' }, auth('target')), { code: 'not-found' });
  const result = await api.getSupportConversation({ ticketId: 'report' }, owner);
  assert.equal(result.initialMessage.message, 'Original report');
  assert.equal(result.conversation.reference, 'MOD-EXAMPLE');
  for (const privateText of ['admin@example.invalid', 'Internal note', 'reportedUid', 'reporterUid']) {
    assert(!JSON.stringify(result).includes(privateText));
  }
  await db.doc('users/owner').update({ accountDeleted: true });
  for (const [name, data] of [
    ['listSupportConversations', {}], ['getSupportConversation', { ticketId: 'report' }],
    ['sendSupportMessage', request()], ['markSupportConversationRead', { ticketId: 'report', seenReplyVersion: 0 }],
  ]) await assert.rejects(api[name](data, owner), { code: 'permission-denied' });
});

test('no direct message reads or writes, forged status/read counters or audit access', async () => {
  await db.doc('supportTickets/report/messages/support').set({ authorType: 'support',
    authorId: 'admin@example.invalid', message: 'Reply', createdAt: Timestamp.now() });
  const own = env.authenticatedContext('owner').firestore();
  const other = env.authenticatedContext('target').firestore();
  await assertFails(getDoc(doc(own, 'supportTickets/report/messages/support')));
  await assertFails(getDoc(doc(own, 'supportTickets/report')));
  await assertFails(getDoc(doc(other, 'supportTickets/report')));
  await assertFails(getDoc(doc(own, 'supportTickets/report/statusHistory/internal')));
  await assertFails(setDoc(doc(own, 'supportTickets/report/messages/forged'), { authorType: 'support', message: 'Fake' }));
  await assertFails(updateDoc(doc(own, 'supportTickets/report'), { userReadReplyVersion: 99, status: 'resolved' }));
  const result = await api.getSupportConversation({ ticketId: 'report' }, owner);
  assert.equal(result.messages[0].message, 'Reply');
  assert.equal(result.messages[0].authorId, undefined);
});

test('existing payout summary remains readable by its owner', async () => {
  await db.doc('supportTickets/payment').set({ userId: 'owner', paymentId: 'p', status: 'open' });
  await assertSucceeds(getDoc(doc(env.authenticatedContext('owner').firestore(), 'supportTickets/payment')));
  const own = env.authenticatedContext('owner').firestore();
  await assertSucceeds(getDocs(query(collection(own, 'supportTickets'),
    where('userId', '==', 'owner'), where('paymentId', '==', 'p'))));
});

test('sending is idempotent, derives author/time/status and rejects forged fields', async () => {
  await assert.rejects(api.sendSupportMessage(request({ authorType: 'support' }), owner), { code: 'invalid-argument' });
  const first = await api.sendSupportMessage(request(), owner);
  assert.equal(first.alreadyExists, false);
  const again = await api.sendSupportMessage(request(), owner);
  assert.equal(again.alreadyExists, true);
  assert.equal(first.messageId, again.messageId);
  assert.equal((await db.doc('supportTickets/report/messages/' + first.messageId).get()).get('authorId'), 'owner');
  assert.equal((await db.collection('supportTickets/report/messages').get()).size, 1);
  assert.equal((await db.collection('supportTickets/report/statusHistory').get()).size, 1);
  await assert.rejects(api.sendSupportMessage(request({ message: 'Different payload' }), owner), { code: 'invalid-argument' });
  for (const message of ['', '   ', 'x'.repeat(2001)]) {
    await assert.rejects(api.sendSupportMessage(request({ message }), owner), { code: 'invalid-argument' });
  }
});

test('simultaneous retries create one user message and one audit event', async () => {
  const responses = await Promise.all(Array.from({ length: 3 }, () => api.sendSupportMessage(request(), owner)));
  assert.equal(responses.filter(response => !response.alreadyExists).length, 1);
  assert.equal((await db.collection('supportTickets/report/messages').get()).size, 1);
  assert.equal((await db.collection('supportTickets/report/statusHistory').get()).size, 1);
});

test('simultaneous distinct messages share the account rate gate', async () => {
  const results = await Promise.allSettled([
    api.sendSupportMessage(request(), owner),
    api.sendSupportMessage(request({ requestId: 'different_request_123' }), owner),
  ]);
  assert.equal(results.filter(result => result.status === 'fulfilled').length, 1);
  assert.equal(results.find(result => result.status === 'rejected').reason.code, 'resource-exhausted');
  assert.equal((await db.collection('supportTickets/report/messages').get()).size, 1);
});

test('waiting tickets return to open; resolved requires explicit reopening; closed rejects new messages', async () => {
  const ref = db.doc('supportTickets/report');
  await ref.update({ status: 'waiting_for_user' });
  await api.sendSupportMessage(request(), owner);
  assert.equal((await ref.get()).get('status'), 'open');
  await ref.update({ status: 'resolved', resolvedAt: Timestamp.now() });
  now += 6000;
  const second = request({ requestId: 'request_abcdefghij' });
  await assert.rejects(api.sendSupportMessage(second, owner), { code: 'failed-precondition' });
  await api.sendSupportMessage({ ...second, reopen: true }, owner);
  assert.equal((await ref.get()).get('status'), 'open');
  const events = await ref.collection('statusHistory').where('eventType', '==', 'ticket_reopened').get();
  assert.equal(events.size, 1);
  await ref.update({ status: 'closed' });
  now += 6000;
  await assert.rejects(api.sendSupportMessage(request({ requestId: 'request_closed_123' }), owner), { code: 'failed-precondition' });
  // A retry of the earlier accepted message must not reopen this closed ticket.
  assert.equal((await api.sendSupportMessage(request(), owner)).alreadyExists, true);
  assert.equal((await ref.get()).get('status'), 'closed');
});

test('legacy replies appear without migration and a stale read cannot clear a new reply', async () => {
  const ref = db.doc('supportTickets/report');
  await ref.update({ latestSupportReply: 'Legacy response', latestSupportReplyAt: Timestamp.fromMillis(now) });
  assert.equal((await api.listSupportConversations({}, owner)).unreadCount, 1);
  const conversation = await api.getSupportConversation({ ticketId: 'report' }, owner);
  assert.equal(conversation.messages[0].message, 'Legacy response');
  assert.equal(conversation.conversation.supportReplyVersion, 1);
  await updateTicket(db, ref, 'reply', 'New reply', 'admin@example.invalid', FieldValue);
  assert.equal((await ref.get()).get('supportReplyVersion'), 2);
  assert.equal((await api.markSupportConversationRead({ ticketId: 'report', seenReplyVersion: 1 }, owner)).unreadCount, 1);
  assert.equal((await api.listSupportConversations({}, owner)).unreadCount, 1);
  assert.equal((await api.markSupportConversationRead({ ticketId: 'report', seenReplyVersion: 0 }, owner)).unreadCount, 1);
  await assert.rejects(api.markSupportConversationRead({ ticketId: 'report', seenReplyVersion: 3 }, owner), { code: 'invalid-argument' });
  await assert.rejects(api.markSupportConversationRead({ ticketId: 'report', seenReplyVersion: 2 }, auth('target')), { code: 'not-found' });
  assert.equal((await api.markSupportConversationRead({ ticketId: 'report', seenReplyVersion: 2 }, owner)).unreadCount, 0);
  const refreshed = await api.getSupportConversation({ ticketId: 'report' }, owner);
  assert.equal(refreshed.messages.length, 2); // Legacy preserved; new response has no synthetic duplicate.
});

test('read races with administrator reply preserve the unseen version', async () => {
  const ref = db.doc('supportTickets/report');
  await updateTicket(db, ref, 'reply', 'First', 'admin', FieldValue);
  await Promise.all([
    api.markSupportConversationRead({ ticketId: 'report', seenReplyVersion: 1 }, owner),
    updateTicket(db, ref, 'reply', 'Second', 'admin', FieldValue),
  ]);
  assert.equal((await api.listSupportConversations({}, owner)).unreadCount, 1);
});

test('pagination is owner-scoped and message pages do not skip equal timestamps', async () => {
  const batch = db.batch();
  for (let i = 0; i < 27; i++) batch.set(db.doc('supportTickets/ticket' + String(i).padStart(2, '0')),
    { userId: 'owner', status: 'open', updatedAt: Timestamp.fromMillis(now + i) });
  batch.set(db.doc('supportTickets/other'), { userId: 'target', status: 'open' });
  for (let i = 0; i < 55; i++) batch.set(db.doc('supportTickets/report/messages/m' + String(i).padStart(2, '0')),
    { authorType: 'user', authorId: 'owner', message: 'Text ' + i, createdAt: Timestamp.fromMillis(now) });
  await batch.commit();
  const first = await api.listSupportConversations({}, owner);
  assert.equal(first.conversations.length, 25);
  const rest = await api.listSupportConversations({ cursor: first.nextCursor }, owner);
  assert.equal(rest.conversations.length, 3);
  assert.equal(rest.nextCursor, null);
  await assert.rejects(api.listSupportConversations({ cursor: 'other' }, owner), { code: 'invalid-argument' });
  const page = await api.getSupportConversation({ ticketId: 'report' }, owner);
  const older = await api.getSupportConversation({ ticketId: 'report', cursor: page.nextCursor }, owner);
  assert.equal(page.messages.length, 50);
  assert.equal(older.messages.length, 5);
  assert.equal(new Set([...page.messages, ...older.messages].map(message => message.messageId)).size, 55);
  assert.equal(older.nextCursor, null);
});

test('missing payment and retention deletion block writes without orphan messages', async () => {
  const ref = db.doc('supportTickets/report');
  await ref.update({ paymentId: 'missing' });
  await assert.rejects(api.sendSupportMessage(request(), owner), { code: 'failed-precondition' });
  assert.equal((await ref.collection('messages').get()).size, 0);
  await ref.update({ retentionDeletionStartedAt: Timestamp.now() });
  await assert.rejects(api.sendSupportMessage(request(), owner), { code: 'failed-precondition' });
  await assert.rejects(api.getSupportConversation({ ticketId: 'report' }, owner), { code: 'failed-precondition' });
  assert.equal((await api.listSupportConversations({}, owner)).conversations.length, 0);
});

test('payout user replies touch the parent payment and retain existing support flow', async () => {
  const ref = db.doc('supportTickets/report');
  await ref.update({ paymentId: 'payment', type: FieldValue.delete() });
  await db.doc('payments/payment').set({ userId: 'owner', status: 'action_required' });
  await api.sendSupportMessage(request(), owner);
  assert((await db.doc('payments/payment').get()).get('supportActivityAt') instanceof Timestamp);
  assert.equal((await db.doc('payments/payment').get()).get('status'), 'action_required');
});

test('rate limits cover short bursts, hourly totals, daily totals and reset', async () => {
  await api.sendSupportMessage(request(), owner);
  await assert.rejects(api.sendSupportMessage(request({ requestId: 'request_second_123' }), owner), { code: 'resource-exhausted' });
  for (let hour = 0; hour < 3; hour++) {
    if (hour) now += 3600000;
    for (let i = hour ? 0 : 1; i < 10; i++) {
      now += 6000;
      await api.sendSupportMessage(request({ requestId: `request_rate_${hour}_${i}_unique` }), owner);
    }
    now += 6000;
    await assert.rejects(api.sendSupportMessage(request({ requestId: `request_limit_${hour}_unique` }), owner),
      { code: 'resource-exhausted' });
  }
  now += 3600000;
  await assert.rejects(api.sendSupportMessage(request({ requestId: 'request_daily_limit' }), owner), { code: 'resource-exhausted' });
  now += 86400000;
  assert.equal((await api.sendSupportMessage(request({ requestId: 'request_next_day_123' }), owner)).ok, true);
});
