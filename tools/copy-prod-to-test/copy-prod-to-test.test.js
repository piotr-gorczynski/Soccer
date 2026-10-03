const test = require('node:test');
const assert = require('node:assert/strict');
const { copyFirestoreCollection, clearFirestoreCollection, createConfiguredBulkWriter } = require('./copy-prod-to-test');
const { Timestamp } = require('firebase-admin/firestore');

// In-memory refs include missing ancestors, as Firestore listDocuments does.
function database(entries = {}, fail = null) {
  const records = new Map(Object.entries(entries));
  let writes = 0;
  function collection(path) {
    return { id: path.split('/').at(-1), doc: id => document(path + '/' + id),
      listDocuments: async () => [...new Set([...records.keys()]
        .filter(k => k.startsWith(path + '/')).map(k => k.slice(path.length + 1).split('/')[0]))]
        .map(id => document(path + '/' + id)) };
  }
  function document(path) {
    return { id: path.split('/').at(-1), path, collection: id => collection(path + '/' + id),
      get: async () => ({ exists: records.has(path), data: () => records.get(path) }),
      listCollections: async () => [...new Set([...records.keys()]
        .filter(k => k.startsWith(path + '/')).map(k => k.slice(path.length + 1).split('/')[0]))]
        .map(id => collection(path + '/' + id)) };
  }
  return { records, collection, bulkWriter: () => ({
    onWriteError: () => {}, close: async () => {},
    set: async (ref, data) => { writes++; if (fail) throw new Error(fail); records.set(ref.path, data); },
    delete: async ref => { writes++; if (fail) throw new Error(fail); records.delete(ref.path); },
  }), writes: () => writes };
}
const timestamp = Timestamp.fromMillis(123456789);
const fixtures = {
  'payments/p': { status: 'sent', recipientDetailsVersion: 2, amount: 320, updatedAt: timestamp },
  'payments/p/private/recipient': { firstName: 'Test', lastName: 'Recipient', walletProvider: 'ROCKET', walletNumber: '017123456789', submittedAt: timestamp },
  'payments/p/statusHistory/e': { from: 'processing', to: 'sent', changedAt: timestamp },
  'payments/p/notificationEvents/n': { sent: true },
  'payments/missing/private/recipient': { firstName: 'Test', lastName: 'Missing', walletProvider: 'BKASH', walletNumber: '01712345678' },
  'payments/p/statusHistory/missing/details/e': { recordedAt: timestamp },
  'supportTickets/s': { paymentId: 'p', status: 'open' },
  'supportTickets/s/messages/m': { text: 'Test message', createdAt: timestamp },
  'supportTickets/s/statusHistory/h': { status: 'open' },
};
test('copies private recipients, histories, support and missing ancestors without flattening or changing values', async () => {
  const source = database(fixtures), target = database();
  assert.equal((await copyFirestoreCollection(source, target, 'payments')).failed, 0);
  assert.equal((await copyFirestoreCollection(source, target, 'supportTickets')).failed, 0);
  assert.deepEqual(target.records, source.records);
  assert.equal(source.writes(), 0);
  assert.equal(target.records.has('payments/missing'), false);
  assert.ok(target.records.get('payments/p').updatedAt instanceof Timestamp);
});
test('clears private descendants including orphaned ancestors and leaves other collections', async () => {
  const target = database(fixtures);
  const result = await clearFirestoreCollection(target, 'payments');
  assert.equal(result.failed, 0);
  assert.ok([...target.records.keys()].every(k => k.startsWith('supportTickets/')));
  assert.equal(target.records.size, 3);
});
test('reports rejected private writes instead of reporting success', async () => {
  const source = database({ 'payments/p/private/recipient': fixtures['payments/p/private/recipient'] });
  const target = database({}, 'write rejected');
  const result = await copyFirestoreCollection(source, target, 'payments');
  assert.equal(result.failed, 1);
  assert.equal(result.success, 0);
});
test('stops scanning after permanent delete failure', async () => {
  const target = database(fixtures, 'delete rejected');
  const result = await clearFirestoreCollection(target, 'payments');
  assert.ok(result.failed > 0);
  assert.deepEqual(target.records, new Map(Object.entries(fixtures)));
});
test('limits retries using BulkWriterError code directly', () => {
  let retry;
  createConfiguredBulkWriter({ bulkWriter: () => ({ onWriteError: callback => { retry = callback; } }) });
  assert.equal(retry({ code: 14, failedAttempts: 1 }), true);
  assert.equal(retry({ code: 14, failedAttempts: 5 }), false);
  assert.equal(retry({ code: 7, failedAttempts: 1 }), false);
});
