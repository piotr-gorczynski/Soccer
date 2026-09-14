const assert = require('node:assert/strict');
const test = require('node:test');

const { derivePrizePool, validateEligibilityMetadata } = require('./create-tournament');

test('derives a backward-compatible prize pool from regulation metadata', () => {
  assert.deepEqual(derivePrizePool({
    prizeRules: {
      cashPrizesEnabled: true,
      currency: 'BDT',
      prizePool: {
        totalAmount: 1000,
        awards: [{ place: 1, amount: 1000 }]
      }
    }
  }), {
    enabled: true,
    currency: 'BDT',
    totalAmount: 1000,
    awards: [{ place: 1, amount: 1000 }],
    firstPlacePrize: 1000
  });
});

test('preserves any number of configured prize places', () => {
  const prizePool = derivePrizePool({
    prizeRules: {
      cashPrizesEnabled: true,
      currency: 'BDT',
      prizePool: {
        totalAmount: 3500,
        awards: [
          { place: 1, amount: 2000 },
          { place: 2, amount: 1000 },
          { place: 3, amount: 500 }
        ]
      }
    }
  });

  assert.equal(prizePool.firstPlacePrize, 2000);
  assert.equal(prizePool.awards.length, 3);
  assert.equal(prizePool.totalAmount, 3500);
});

test('writes a disabled prize pool for a non-cash regulation', () => {
  assert.deepEqual(derivePrizePool({ prizeRules: { cashPrizesEnabled: false } }), {
    enabled: false
  });
});

test('rejects inconsistent total and award amounts', () => {
  assert.throws(() => derivePrizePool({
    prizeRules: {
      cashPrizesEnabled: true,
      currency: 'BDT',
      prizePool: {
        totalAmount: 1000,
        awards: [{ place: 1, amount: 900 }]
      }
    }
  }), /must add up/);
});

test('validates eligibility metadata required by a cash-prize regulation', () => {
  assert.deepEqual(validateEligibilityMetadata({
    market: 'BD',
    minimumAge: 18,
    prizeRules: {
      cashPrizesEnabled: true,
      payoutMethods: ['bkash', 'nagad']
    }
  }), {
    market: 'BD',
    minimumAge: 18,
    payoutMethods: ['bkash', 'nagad']
  });
});

test('does not require eligibility metadata for legacy and non-cash regulations', () => {
  assert.equal(validateEligibilityMetadata({}), null);
  assert.equal(validateEligibilityMetadata({
    prizeRules: { cashPrizesEnabled: false }
  }), null);
});

test('rejects invalid cash-prize eligibility metadata', () => {
  const valid = {
    market: 'BD',
    minimumAge: 18,
    prizeRules: {
      cashPrizesEnabled: true,
      payoutMethods: ['bkash', 'nagad']
    }
  };

  assert.throws(() => validateEligibilityMetadata({ ...valid, market: 'Bangladesh' }), /market/);
  assert.throws(() => validateEligibilityMetadata({ ...valid, minimumAge: 0 }), /minimumAge/);
  assert.throws(() => validateEligibilityMetadata({
    ...valid,
    prizeRules: { ...valid.prizeRules, payoutMethods: [] }
  }), /payout method/);
  assert.throws(() => validateEligibilityMetadata({
    ...valid,
    prizeRules: { ...valid.prizeRules, payoutMethods: ['bkash', 'bkash'] }
  }), /duplicate/);
});
