'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { assertTransition, validateTransitionData, buildAdminHistory, recordRecipientSubmission } = require('./payment-workflow');

test('allows the normal payout lifecycle', () => {
  assert.doesNotThrow(() => assertTransition('ready_for_processing', 'processing'));
  assert.doesNotThrow(() => assertTransition('processing', 'sent'));
  assert.doesNotThrow(() => assertTransition('sent', 'completed'));
});

test('admin history preserves supplied text and transfer metadata independently of later edits', () => {
  const input = {
    issueCode: 'invalid_recipient_account', userMessage: '  Popraw konto\nSUP-123  ',
    notes: 'Internal note', provider: 'Remitly', providerReference: 'R-123', clearIssue: false,
    unrelated: 'must not be recorded',
  };
  const history = buildAdminHistory('processing', 'action_required', input,
    'admin@example.test', 'test', 'SERVER_TIME');
  input.userMessage = 'Replacement';
  assert.deepEqual(history.details, {
    issueCode: 'invalid_recipient_account', userMessage: '  Popraw konto\nSUP-123  ',
    notes: 'Internal note', provider: 'Remitly', providerReference: 'R-123', clearIssue: false,
  });
  assert.equal(history.changedAt, 'SERVER_TIME');
  assert.equal(history.changedBy, 'admin@example.test');
  assert.equal(history.from, 'processing');
  assert.equal(history.to, 'action_required');
});

test('recipient submissions use event time and survive repeated/out-of-order trigger delivery', async () => {
  const records = new Map();
  const paymentRef = { collection: () => ({ doc: id => ({ id }) }) };
  const db = { runTransaction: callback => callback({
    get: async ref => ({ exists: records.has(ref.id) }),
    set: (ref, value) => records.set(ref.id, value),
  }) };
  const fields = { serverTimestamp: () => 'RECORDING_TIME' };
  const before = { status: 'action_required', recipientInfo: { accountNumber: 'old' } };
  const after = { status: 'ready_for_processing', userId: 'winner',
    statusUpdatedAt: 'SUBMISSION_TIME', recipientInfo: { accountNumber: 'new' } };
  await recordRecipientSubmission(db, paymentRef, before, after, 'second', fields);
  const recorded = records.get('recipient-second');
  await recordRecipientSubmission(db, paymentRef, before, after, 'second', fields);
  await recordRecipientSubmission(db, paymentRef, { status: 'awaiting_details' },
    { ...after, statusUpdatedAt: 'EARLIER_TIME' }, 'first', fields);
  assert.equal(records.size, 2);
  assert.equal(records.get('recipient-second'), recorded);
  assert.equal(recorded.changedAt, 'SUBMISSION_TIME');
  assert.equal(recorded.recordedAt, 'RECORDING_TIME');
  assert.equal(recorded.previousRecipientInfo.accountNumber, 'old');
  assert.equal(recorded.recipientInfo.accountNumber, 'new');
  assert.equal(recorded.changedBy, 'winner');
  assert.equal(records.get('recipient-first').changedAt, 'EARLIER_TIME');
});

test('admin transitions are not duplicated by recipient history recorder', async () => {
  const db = { runTransaction: () => assert.fail('Unexpected history write') };
  await recordRecipientSubmission(db, null, { status: 'ready_for_processing' },
    { status: 'action_required' }, 'event', {});
});

test('allows retryable errors but rejects retired and invalid transitions', () => {
  assert.doesNotThrow(() => assertTransition('processing', 'action_required'));
  assert.throws(() => assertTransition('pending', 'processing'), /Cannot transition/);
  assert.throws(() => assertTransition('ready_for_processing', 'completed'), /Cannot transition/);
  assert.throws(() => assertTransition('completed', 'processing'), /Cannot transition/);
});

test('requires provider data for sent and a safe issue for action_required', () => {
  assert.throws(() => validateTransitionData('sent', {}), /provider/);
  assert.doesNotThrow(() => validateTransitionData('sent', {
    provider: 'remitly', providerReference: 'R-123'
  }));
  assert.throws(() => validateTransitionData('action_required', {}), /issueCode/);
  assert.doesNotThrow(() => validateTransitionData('action_required', {
    issueCode: 'invalid_recipient_account', userMessage: 'Check your account number.'
  }));
});
