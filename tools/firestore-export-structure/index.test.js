const test = require('node:test');
const assert = require('node:assert/strict');
const admin = require('firebase-admin');
const { describeValue, describeCollectionRecursive, main } = require('./index');

test('nested maps and mixed arrays retain structure without values', () => {
  const shape = describeValue({ recipient: { firstName: 'PRIVATE' }, history: [{ status: 'sent' }, { status: 'ready' }, null, 3] });
  assert.equal(shape.fields.recipient.fields.firstName.type, 'string');
  assert.equal(shape.fields.history.elements.length, 3);
  assert.ok(!JSON.stringify(shape).includes('PRIVATE'));
  assert.deepEqual(describeValue(admin.firestore.Timestamp.now()), { type: 'timestamp' });
  assert.deepEqual(describeValue(Buffer.from('secret')), { type: 'bytes' });
});

function document(path, data, children = []) {
  return { path, id: path.split('/').pop(),
    async get() { return { exists: data !== undefined, data: () => data }; },
    async listCollections() { return children; } };
}
function collection(path, refs) { return { path, async listDocuments() { return refs; } }; }

test('visits missing parents and private recipient with limit per collection', async () => {
  const recipient = collection('payments/a/private', [document('payments/a/private/recipient', { firstName: 'secret', submittedAt: admin.firestore.Timestamp.now() })]);
  const ignored = document('payments/b', {});
  ignored.get = async () => { throw new Error('Exceeded limit'); };
  const root = collection('payments', [ignored, document('payments/a', undefined, [recipient])]);
  const lines = [];
  await describeCollectionRecursive(root, 1, '', line => lines.push(line));
  const output = lines.join('\n');
  assert.match(output, /a \[missing parent\]/);
  assert.match(output, /payments\/a\/private/);
  assert.match(output, /firstName: string/);
  assert.match(output, /submittedAt: timestamp/);
  assert.ok(!output.includes('secret'));
});

test('read failures propagate instead of reporting successful export', async () => {
  await assert.rejects(describeCollectionRecursive({ path: 'users', async listDocuments() { throw new Error('denied'); } }, 1), /denied/);
});

test('rejects malformed limits before accessing credentials', async () => {
  for (const limit of ['1abc', '0', '-1', '1.5', '9007199254740992']) {
    await assert.rejects(main(['dev', limit]), /Usage/);
  }
});
