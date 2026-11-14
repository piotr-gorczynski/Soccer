'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { replyVersion, readVersion, publicMessage, publicTicket, handlers } = require('./conversation');

test('inbox counts unread conversations independently of reply totals including resolved tickets', async () => {
  const docs = [
    { status: 'resolved', supportReplyVersion: 4, userReadReplyVersion: 1 },
    { status: 'open', supportReplyVersion: 2, userReadReplyVersion: 0 },
    { status: 'open', supportReplyVersion: 1, userReadReplyVersion: 1 },
  ].map((data,i) => ({ id: String(i), data: () => data, get: key => data[key] }));
  const db = { collection: name => name === 'users'
    ? { doc: () => ({ get: async () => ({ exists: true, get: () => false }) }) }
    : { where: () => ({ get: async () => ({ docs }) }) } };
  const result = await handlers(db, {}, Error).listSupportConversations({}, { auth: { uid: 'owner' } });
  assert.equal(result.unreadConversationCount,2); assert.equal(result.unreadCount,5);
});

test('legacy replies are unread without a migration; read versions are bounded', () => {
  assert.equal(replyVersion({}), 0);
  assert.equal(replyVersion({ latestSupportReply: 'Reply' }), 1);
  assert.equal(readVersion({ latestSupportReply: 'Reply', userReadReplyVersion: 99 }), 1);
  assert.equal(readVersion({ supportReplyVersion: 2, userReadReplyVersion: -1 }), 0);
});

test('public conversation projections omit internal identities, audit and payment data', () => {
  const secret = { userId: 'owner-secret', reporterUid: 'owner-secret', reportedUid: 'target-secret',
    authorId: 'admin@example.invalid', paymentId: 'private-payment', retentionHold: { reason: 'private' },
    source: 'internal', historyId: 'audit', status: 'open', reference: 'MOD-TEST',
    authorType: 'support', message: 'Public reply', latestSupportReply: 'Public reply' };
  const snapshot = { id: 'ticket', data: () => secret };
  const encoded = JSON.stringify([publicTicket(snapshot), publicMessage(snapshot)]);
  for (const value of ['owner-secret', 'target-secret', 'admin@example.invalid', 'private-payment', 'audit']) {
    assert(!encoded.includes(value));
  }
  assert(encoded.includes('Public reply'));
  assert.equal(publicTicket(snapshot).unreadCount, 1);
});

test('internal messages are not presented as conversation messages', () => {
  assert.equal(publicMessage({ id: 'x', data: () => ({ authorType: 'admin_note', message: 'internal' }) }), null);
});

test('all conversation endpoints require authentication before touching the database', async () => {
  class HttpsError extends Error { constructor(code, message) { super(message); this.code = code; } }
  const api = handlers({}, {}, HttpsError);
  for (const endpoint of Object.values(api)) {
    await assert.rejects(endpoint({}, {}), { code: 'unauthenticated' });
  }
});
