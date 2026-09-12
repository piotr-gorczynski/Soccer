'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');
const {
  createRegulation,
  parseArguments,
  validateRegulation,
} = require('./create-regulation');

function validCashRegulation() {
  return {
    name: 'Bangladesh Cash Tournament Rules',
    status: 'active',
    market: 'BD',
    minimumAge: 18,
    prizeRules: {
      cashPrizesEnabled: true,
      currency: 'BDT',
      payoutMethods: ['bkash', 'nagad'],
      prizePool: {
        totalAmount: 1000,
        awards: [{ place: 1, amount: 1000 }],
      },
    },
    translations: {
      en: { rules: ['Participation is free.', 'Players must be adults.'] },
      bn: { rules: ['অংশগ্রহণ বিনামূল্যে।'] },
    },
  };
}

test('validates a Bangladesh cash-prize regulation', () => {
  const result = validateRegulation(validCashRegulation());

  assert.equal(result.root.market, 'BD');
  assert.equal(result.root.minimumAge, 18);
  assert.deepEqual(result.root.prizeRules.payoutMethods, ['bkash', 'nagad']);
  assert.deepEqual(result.root.prizeRules.prizePool, {
    totalAmount: 1000,
    awards: [{ place: 1, amount: 1000 }],
  });
  assert.match(result.root.body, /^• Participation is free\./);
  assert.deepEqual(result.translations.bn.rules, ['অংশগ্রহণ বিনামূল্যে।']);
});

test('keeps legacy-compatible optional market and prize fields', () => {
  const result = validateRegulation({
    name: 'General Tournament Game Rules',
    status: 'active',
    translations: { en: { rules: ['General rule.'] } },
  });

  assert.equal('market' in result.root, false);
  assert.equal('minimumAge' in result.root, false);
  assert.equal('prizeRules' in result.root, false);
});

test('requires a market and minimum age for cash prizes', () => {
  const input = validCashRegulation();
  delete input.market;
  assert.throws(() => validateRegulation(input), /market.*required/i);

  input.market = 'BD';
  delete input.minimumAge;
  assert.throws(() => validateRegulation(input), /minimumAge.*required/i);
});

test('rejects invalid currency and duplicate payout methods', () => {
  const invalidCurrency = validCashRegulation();
  invalidCurrency.prizeRules.currency = 'bdt';
  assert.throws(() => validateRegulation(invalidCurrency), /currency/i);

  const duplicateMethods = validCashRegulation();
  duplicateMethods.prizeRules.payoutMethods = ['bkash', 'bkash'];
  assert.throws(() => validateRegulation(duplicateMethods), /duplicates/i);
});

test('supports an arbitrary list of prize-winning places', () => {
  const input = validCashRegulation();
  input.prizeRules.prizePool = {
    totalAmount: 3500,
    awards: [
      { place: 3, amount: 500 },
      { place: 1, amount: 2000 },
      { place: 2, amount: 1000 },
    ],
  };

  const result = validateRegulation(input);
  assert.deepEqual(result.root.prizeRules.prizePool.awards, [
    { place: 1, amount: 2000 },
    { place: 2, amount: 1000 },
    { place: 3, amount: 500 },
  ]);
});

test('rejects missing, inconsistent, or duplicate prize allocations', () => {
  const missingPool = validCashRegulation();
  delete missingPool.prizeRules.prizePool;
  assert.throws(() => validateRegulation(missingPool), /prizePool.*object/i);

  const inconsistentPool = validCashRegulation();
  inconsistentPool.prizeRules.prizePool.totalAmount = 2000;
  assert.throws(() => validateRegulation(inconsistentPool), /sum.*equal/i);

  const duplicatePlaces = validCashRegulation();
  duplicatePlaces.prizeRules.prizePool = {
    totalAmount: 1500,
    awards: [
      { place: 1, amount: 1000 },
      { place: 1, amount: 500 },
    ],
  };
  assert.throws(() => validateRegulation(duplicatePlaces), /duplicate places/i);
});

test('supports dry-run command arguments', () => {
  assert.deepEqual(
    parseArguments(['dev', 'regulation.json', '--dry-run']),
    { environment: 'dev', jsonPath: 'regulation.json', dryRun: true }
  );
});

test('creates the root and translations in one batch using a native ID', async () => {
  const writes = [];
  let committed = false;
  const regulationRef = {
    id: 'nativeFirestoreId',
    collection(language) {
      return {
        doc(documentId) {
          return { path: `regulations/nativeFirestoreId/${language}/${documentId}` };
        },
      };
    },
  };
  const batch = {
    create(reference, data) {
      writes.push({ reference, data });
      return this;
    },
    async commit() {
      committed = true;
    },
  };
  const db = {
    collection(name) {
      assert.equal(name, 'regulations');
      return {
        doc() {
          return regulationRef;
        },
      };
    },
    batch() {
      return batch;
    },
  };
  const serverTimestamp = Symbol('serverTimestamp');
  const admin = {
    firestore: {
      FieldValue: { serverTimestamp: () => serverTimestamp },
    },
  };

  const regulation = validateRegulation(validCashRegulation());
  const id = await createRegulation(db, admin, regulation);

  assert.equal(id, 'nativeFirestoreId');
  assert.equal(committed, true);
  assert.equal(writes.length, 3);
  assert.equal(writes[0].reference, regulationRef);
  assert.equal(writes[0].data.createdAt, serverTimestamp);
  assert.equal(writes[1].reference.path, 'regulations/nativeFirestoreId/en/rules');
  assert.equal(writes[2].reference.path, 'regulations/nativeFirestoreId/bn/rules');
  assert.equal(writes[1].data.updatedAt, serverTimestamp);
});
