const assert = require('node:assert/strict');
const test = require('node:test');

const { derivePrizePool } = require('./create-tournament');

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
