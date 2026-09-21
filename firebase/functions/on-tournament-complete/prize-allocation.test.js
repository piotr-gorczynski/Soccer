'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { calculatePayouts } = require('./prize-allocation');

test('splits a single first-place prize between players tied on points', () => {
  const standings = [
    { userId: 'player-b', points: 3 },
    { userId: 'player-a', points: 3 },
    { userId: 'player-c', points: 0 },
  ];
  const prizePool = {
    totalAmount: 1000,
    awards: [{ place: 1, amount: 1000 }],
  };

  assert.deepEqual(calculatePayouts(standings, prizePool), [
    { userId: 'player-b', rank: 1, amount: 500, tied: true, tieCount: 2, points: 3 },
    { userId: 'player-a', rank: 1, amount: 500, tied: true, tieCount: 2, points: 3 },
  ]);
});

test('combines occupied prize positions for a first-place tie', () => {
  const standings = [
    { userId: 'a', points: 6 },
    { userId: 'b', points: 6 },
    { userId: 'c', points: 3 },
  ];
  const prizePool = {
    totalAmount: 3500,
    awards: [
      { place: 1, amount: 2000 },
      { place: 2, amount: 1000 },
      { place: 3, amount: 500 },
    ],
  };

  assert.deepEqual(calculatePayouts(standings, prizePool), [
    { userId: 'a', rank: 1, amount: 1500, tied: true, tieCount: 2, points: 6 },
    { userId: 'b', rank: 1, amount: 1500, tied: true, tieCount: 2, points: 6 },
    { userId: 'c', rank: 3, amount: 500, tied: false, tieCount: 1, points: 3 },
  ]);
});

test('rounds down and excludes zero-score participants', () => {
  const standings = [
    { userId: 'a', points: 3 },
    { userId: 'b', points: 3 },
    { userId: 'c', points: 3 },
    { userId: 'd', points: 0 },
  ];
  const prizePool = {
    totalAmount: 1000,
    awards: [{ place: 1, amount: 1000 }],
  };

  const payouts = calculatePayouts(standings, prizePool);
  assert.equal(payouts.length, 3);
  assert.ok(payouts.every(payout => payout.amount === 333));
  assert.equal(payouts.reduce((sum, payout) => sum + payout.amount, 0), 999);
});

test('supports the legacy firstPlacePrize field', () => {
  const payouts = calculatePayouts(
    [{ userId: 'winner', points: 3 }],
    { totalAmount: 1000, firstPlacePrize: 1000 }
  );

  assert.deepEqual(payouts, [
    { userId: 'winner', rank: 1, amount: 1000, tied: false, tieCount: 1, points: 3 },
  ]);
});
