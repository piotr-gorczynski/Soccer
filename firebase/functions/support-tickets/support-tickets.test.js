'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { validateCreateRequest } = require('./support-ticket');
const { handlers } = require('./attention');

test('report response distinguishes an existing open report without writing a duplicate', async () => {
  const writes = [];
  let existing = [];
  const ref = path => ({ id: path.split('/').pop(), path,
    collection: name => ref(path + '/' + name),
    doc: (id = 'new-ticket') => ref(path + '/' + id), where() { return this; } });
  const db = { collection: ref, runTransaction: fn => fn({
    get: async r => r.path === 'supportTickets' ? { docs: existing }
      : { exists: true, id: r.id, get: key => key === 'nickname' ? 'Player' : undefined },
    set: (r, data) => writes.push({ path: r.path, data }),
  }) };
  const moderation = require('./moderation').handlers(db, { serverTimestamp: () => 123 }, Error);
  const request = { reportedUid: 'target', reason: 'Report', context: 'friends' };
  const auth = { auth: { uid: 'reporter' } };
  const created = await moderation.reportPlayer(request, auth);
  assert.equal(created.alreadyExists, false);
  assert.equal(writes.filter(w => w.path === 'supportTickets/new-ticket').length, 1);
  existing = [{ get: key => ({ status: 'open', reportedUid: 'target', reference: created.reference })[key] }];
  writes.length = 0;
  assert.deepEqual(await moderation.reportPlayer(request, auth), {
    reference: created.reference, alreadyExists: true,
  });
  assert.equal(writes.length, 0);
});

function snapshot(data, parent) {
  return { exists: data != null, get: key => data?.[key],
    ref: { parent: { parent: { get: async () => snapshot(parent) } } } };
}

const eventContext = { eventId: 'event-1', params: { ticketId: 'ticket-1', messageId: 'reply-1' } };

test('committed payout and moderation tickets emit only allowlisted log fields', async () => {
  const logs = [];
  const handler = handlers({ info: (...args) => logs.push(args) });
  for (const type of [undefined, 'moderation']) {
    await handler.ticketCreated(snapshot({ status: 'open', type, userId: 'private-uid',
      message: 'private message', nickname: 'private name', walletNumber: 'private wallet' }), eventContext);
  }
  assert.equal(logs.length, 2);
  for (const [message, fields] of logs) {
    assert.equal(message, 'Support requires attention');
    assert.deepEqual(fields, { event: 'support_attention_required', reason: 'ticket_created',
      ticketId: 'ticket-1', eventId: 'event-1' });
  }
});

test('a subsequent owner reply emits user_reply', async () => {
  const logs = [];
  await handlers({ info: (...args) => logs.push(args) }).messageCreated(
    snapshot({ authorType: 'user', authorId: 'owner', message: 'private' },
      { userId: 'owner', status: 'open' }), eventContext);
  assert.deepEqual(logs[0][1], { event: 'support_attention_required', reason: 'user_reply',
    ticketId: 'ticket-1', eventId: 'event-1' });
});

test('initial messages, admin replies, non-owners and deleted/closed tickets do not alert', async () => {
  const handler = handlers({ info: () => assert.fail('Unexpected alert') });
  const owner = { userId: 'owner', status: 'open' };
  await handler.messageCreated(snapshot({ authorType: 'user', authorId: 'owner' }, owner),
    { ...eventContext, params: { ...eventContext.params, messageId: 'initial' } });
  await handler.messageCreated(snapshot({ authorType: 'support', authorId: 'owner' }, owner), eventContext);
  await handler.messageCreated(snapshot({ authorType: 'user', authorId: 'other' }, owner), eventContext);
  await handler.messageCreated(snapshot({ authorType: 'user', authorId: 'owner' }, null), eventContext);
  for (const status of ['closed', 'resolved']) {
    await handler.ticketCreated(snapshot({ status }), eventContext);
    await handler.messageCreated(snapshot({ authorType: 'user', authorId: 'owner' },
      { ...owner, status }), eventContext);
  }
});

test('normalizes a valid support request', () => {
  const result = validateCreateRequest({
    paymentId: ' payment-1 ', category: 'validation_rejected', message: ' Help ',
  });
  assert.equal(result.paymentId, 'payment-1');
  assert.equal(result.message, 'Help');
});

test('rejects unsupported categories and oversized messages', () => {
  assert.throws(() => validateCreateRequest({ paymentId: 'p', category: 'invalid' }), /Unknown/);
  assert.throws(() => validateCreateRequest({
    paymentId: 'p', category: 'other', message: 'x'.repeat(1001),
  }), /too long/);
});
