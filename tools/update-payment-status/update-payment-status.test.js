'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { parseArgs } = require('./update-payment-status');

test('parses a processing transition', () => {
  assert.deepEqual(parseArgs(['dev', 'payment-1', 'processing', '--dry-run']), {
    env: 'dev', paymentId: 'payment-1', status: 'processing', data: {}, dryRun: true
  });
});

test('parses sent provider metadata', () => {
  const result = parseArgs([
    'prod', 'payment-1', 'sent', '--provider', 'remitly', '--reference', 'R-123'
  ]);
  assert.equal(result.data.provider, 'remitly');
  assert.equal(result.data.providerReference, 'R-123');
});

test('rejects unsupported environments and incomplete options', () => {
  assert.throws(() => parseArgs(['local', 'p1', 'processing']), /Usage/);
  assert.throws(() => parseArgs(['dev', 'p1', 'sent', '--provider']), /Invalid/);
});
