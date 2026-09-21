'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { validateCreateRequest } = require('./support-ticket');

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
