'use strict';

/**
 * Calculate cash payouts from the advertised award positions.
 *
 * Participants with the same positive final score form one tie group. The
 * group consumes consecutive ranking positions, combines the prizes assigned
 * to those positions and splits the combined amount equally (rounded down).
 */
function calculatePayouts(standings, prizePool) {
  const configuredAwards = Array.isArray(prizePool?.awards)
    ? prizePool.awards
    : [];
  const awards = configuredAwards.length > 0
    ? configuredAwards
    : Number(prizePool?.firstPlacePrize) > 0
      ? [{ place: 1, amount: Number(prizePool.firstPlacePrize) }]
      : [];

  const awardByPlace = new Map();
  for (const award of awards) {
    const place = Number(award?.place);
    const amount = Number(award?.amount);
    if (Number.isInteger(place) && place > 0 && Number.isFinite(amount) && amount > 0) {
      awardByPlace.set(place, amount);
    }
  }

  if (awardByPlace.size === 0) return [];

  const eligible = standings.filter(entry => Number(entry.points) > 0);
  const payouts = [];
  let currentPosition = 1;

  for (let index = 0; index < eligible.length;) {
    const score = eligible[index].points;
    const tiedPlayers = [];
    while (index < eligible.length && eligible[index].points === score) {
      tiedPlayers.push(eligible[index]);
      index += 1;
    }

    let combinedPrize = 0;
    for (let offset = 0; offset < tiedPlayers.length; offset += 1) {
      combinedPrize += awardByPlace.get(currentPosition + offset) || 0;
    }

    const amountPerPlayer = Math.floor(combinedPrize / tiedPlayers.length);
    if (amountPerPlayer > 0) {
      for (const player of tiedPlayers) {
        payouts.push({
          userId: player.userId,
          rank: currentPosition,
          amount: amountPerPlayer,
          tied: tiedPlayers.length > 1,
          points: player.points,
        });
      }
    }

    currentPosition += tiedPlayers.length;
  }

  const advertisedTotal = Number(prizePool?.totalAmount) ||
    [...awardByPlace.values()].reduce((sum, amount) => sum + amount, 0);
  const payoutTotal = payouts.reduce((sum, payout) => sum + payout.amount, 0);
  if (payoutTotal > advertisedTotal) {
    throw new Error(`Calculated payout ${payoutTotal} exceeds prize pool ${advertisedTotal}`);
  }

  return payouts;
}

module.exports = { calculatePayouts };
