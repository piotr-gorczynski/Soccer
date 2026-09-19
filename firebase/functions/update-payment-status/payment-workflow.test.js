'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { assertTransition, validateTransitionData } = require('./payment-workflow');

test('allows the normal payout lifecycle', () => {
  assert.doesNotThrow(() => assertTransition('ready_for_processing', 'processing'));
  assert.doesNotThrow(() => assertTransition('processing', 'sent'));
  assert.doesNotThrow(() => assertTransition('sent', 'completed'));
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
