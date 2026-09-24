'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { updateTicket } = require('./support-tickets');

function fixture(status = 'open', exists = true) {
  let nextId = 0;
  let state = { status, reference: 'SUP-TEST' };
  const records = new Map();
  const ticketRef = { id: 'ticket', collection: name => ({ doc: () => ({
    id: String(++nextId), path: `${name}/${nextId}`,
  }) }) };
  const fields = { serverTimestamp: () => 'SERVER_TIME', delete: () => 'DELETE' };
  const db = { runTransaction: async callback => {
    const pending = [];
    const result = await callback({
      get: async () => ({ exists, get: key => state[key] }),
      update: (ref, data) => pending.push(() => { state = { ...state, ...data }; }),
      set: (ref, data) => pending.push(() => records.set(ref.path, data)),
    });
    pending.forEach(commit => commit());
    return result;
  } };
  return { db, ticketRef, fields, records, state: () => state };
}

test('successive replies preserve all texts, authors, timestamps and same-status events', async () => {
  const f = fixture();
  await updateTicket(f.db, f.ticketRef, 'reply', '  First\nreply  ', 'admin1', f.fields);
  await updateTicket(f.db, f.ticketRef, 'reply', 'Second reply', 'admin2', f.fields);
  const history = [...f.records].filter(([key]) => key.startsWith('statusHistory/')).map(([, value]) => value);
  const messages = [...f.records].filter(([key]) => key.startsWith('messages/')).map(([, value]) => value);
  assert.deepEqual(history.map(h => [h.from, h.to]), [
    ['open', 'waiting_for_user'], ['waiting_for_user', 'waiting_for_user'],
  ]);
  assert.equal(history[0].message, '  First\nreply  ');
  assert.equal(history[1].changedBy, 'admin2');
  assert.equal(history[0].changedAt, 'SERVER_TIME');
  assert.equal(messages[0].message, history[0].message);
  assert.equal(messages[0].createdAt, history[0].changedAt);
  assert.equal(f.state().latestSupportReply, 'Second reply');
  assert.equal(f.state().statusUpdatedAt, 'SERVER_TIME');
  assert.equal(f.state().updatedAt, 'SERVER_TIME');
});

test('resolve without text still leaves a dated audit event', async () => {
  const f = fixture('waiting_for_user');
  await updateTicket(f.db, f.ticketRef, 'resolve', '', 'admin', f.fields);
  assert.equal(f.records.size, 1);
  const history = [...f.records.values()][0];
  assert.equal(history.from, 'waiting_for_user');
  assert.equal(history.to, 'resolved');
  assert.equal(history.messageId, null);
  assert.equal(history.command, 'resolve');
  assert.equal(f.state().resolvedAt, 'SERVER_TIME');
});

test('reply after resolution preserves the resolution event while clearing current resolvedAt', async () => {
  const f = fixture();
  await updateTicket(f.db, f.ticketRef, 'resolve', 'Done', 'admin', f.fields);
  await updateTicket(f.db, f.ticketRef, 'reply', 'Follow-up', 'admin', f.fields);
  assert.equal(f.state().resolvedAt, 'DELETE');
  assert.equal(f.state().status, 'waiting_for_user');
  assert.equal([...f.records.values()].filter(r => r.eventType === 'ticket_resolved').length, 1);
});

test('missing tickets and invalid commands do not write messages or history', async () => {
  const f = fixture('open', false);
  await assert.rejects(updateTicket(f.db, f.ticketRef, 'reply', 'Text', 'admin', f.fields), /does not exist/);
  await assert.rejects(updateTicket(f.db, f.ticketRef, 'reply', ' ', 'admin', f.fields), /Invalid/);
  await assert.rejects(updateTicket(f.db, f.ticketRef, 'delete', '', 'admin', f.fields), /Unknown/);
  assert.equal(f.records.size, 0);
});
