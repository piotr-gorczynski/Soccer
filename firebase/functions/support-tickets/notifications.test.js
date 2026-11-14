'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { handler } = require('./notifications');

function fixture(user = { fcmToken: 'secret-token' }) {
  const records = new Map(), sends = [], logs = [];
  const snapshot = data => ({ exists: data != null, get: key => data?.[key] });
  const eventRef = id => ({ id, set: async data => records.set(id, { ...records.get(id), ...data }) });
  let chain = Promise.resolve();
  const userRef = { get: async () => snapshot(user) };
  const db = { collection: () => ({ doc: () => userRef }), runTransaction: callback => {
    const result = chain.then(() => callback({
      get: async ref => ref === userRef ? snapshot(user) : snapshot(records.get(ref.id)),
      set: (ref, data) => records.set(ref.id, { ...records.get(ref.id), ...data }),
      update: (ref, data) => Object.assign(user, data),
    })); chain = result.catch(() => {}); return result;
  } };
  const messaging = { send: async payload => { sends.push(payload); return 'accepted'; } };
  const run = handler(db, messaging, { serverTimestamp: () => 'SERVER', delete: () => 'DELETE' },
    { info: (...args) => logs.push(args) });
  const before = { status: 'open', userId: 'secret-user', type: 'moderation' };
  const after = { ...before, status: 'resolved', latestSupportReply: 'private reply', supportReplyVersion: 1 };
  const change = (a = after, b = before) => ({ before: { data: () => b },
    after: { data: () => a, ref: { collection: () => ({ doc: eventRef }) } } });
  const context = { eventId: 'event', params: { ticketId: 'ticket' } };
  return { records, sends, logs, messaging, user, before, after, run, change, context };
}

test('moderation reply plus resolution sends one generic conversation push', async () => {
  const f = fixture(); await f.run(f.change(), f.context);
  assert.equal(f.sends.length, 1);
  assert.equal(f.sends[0].data.ticketId, 'ticket');
  assert.equal(f.sends[0].data.destination, 'help_centre');
  assert.equal(f.sends[0].data.replyVersion, '1');
  const publicData = JSON.stringify([f.sends[0].data, f.logs]);
  for (const secret of ['private reply', 'secret-user', 'secret-token']) assert(!publicData.includes(secret));
  assert.equal(f.records.get('reply-1').sent, true);
});
test('payout and general replies use the same notification path', async () => {
  for (const type of [undefined, 'support']) {
    const f = fixture(); await f.run(f.change({ ...f.after, type, paymentId: 'payment', tournamentId: 'cup' }), f.context);
    assert.equal(f.sends.length, 1); assert.equal(f.sends[0].data.type, 'support_ticket_updated');
  }
});
test('status-only changes and read receipts never send', async () => {
  const f = fixture();
  await f.run(f.change({ ...f.before, status: 'resolved' }), f.context);
  await f.run(f.change({ ...f.after, userReadReplyVersion: 1 }, f.after), f.context);
  assert.equal(f.sends.length, 0);
});
test('concurrent invocations and replay of a sent version do not send twice', async () => {
  const f = fixture(); await Promise.all([f.run(f.change(), f.context), f.run(f.change(), f.context)]);
  await f.run(f.change(), { ...f.context, eventId: 'retry' });
  assert.equal(f.sends.length, 1);
});
test('identical reply text with a new version still sends a new reply', async () => {
  const f = fixture(); await f.run(f.change(), f.context);
  await f.run(f.change({ ...f.after, supportReplyVersion: 2 }, f.after), f.context);
  assert.equal(f.sends.length, 2);
});
test('missing/deleted users and absent tokens skip delivery with a controlled outcome', async () => {
  for (const user of [null, {}, { accountDeleted: true, fcmToken: 'private' }]) {
    const f = fixture(user); await f.run(f.change(), f.context); assert.equal(f.sends.length, 0);
    assert.equal(f.records.get('reply-1').terminal, true);
  }
});
test('invalid token cleanup does not delete a newly registered token', async () => {
  const f = fixture(); f.messaging.send = async () => {
    f.user.fcmToken = 'replacement'; throw Object.assign(new Error('private error'), { code: 'messaging/registration-token-not-registered' });
  };
  await f.run(f.change(), f.context);
  assert.equal(f.user.fcmToken, 'replacement'); assert.equal(f.records.get('reply-1').outcome, 'invalid_token');
  assert(!JSON.stringify(f.logs).includes('private error'));
});
test('failed send releases the claim for retry; expired claims can be retried', async () => {
  const f = fixture(); f.messaging.send = async () => { throw new Error('unavailable'); };
  await assert.rejects(f.run(f.change(), f.context));
  assert.equal(f.records.get('reply-1').leaseUntil, 0);
  f.messaging.send = async payload => f.sends.push(payload);
  await f.run(f.change(), f.context); assert.equal(f.sends.length, 1);
});
test('expired abandoned claims recover on redelivery', async () => {
  const f = fixture(); f.records.set('reply-1', { sent: false, leaseUntil: Date.now() - 1 });
  await f.run(f.change(), f.context); assert.equal(f.sends.length, 1);
});
