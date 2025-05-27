const functions = require("firebase-functions");
const admin     = require("firebase-admin");
const db        = admin.firestore();

/**
 * Ensures the opponent wins if the active player’s clock already elapsed
 * but no client wrote the result.
 */
exports.clockTimeout = functions.firestore
    .document("matches/{matchId}")
    .onUpdate(async (change, context) => {

  const before = change.before.data();
  const after  = change.after.data();

  // only act while match is still active and undecided
  if (after.status !== "active" || after.winner) return null;

  const turn          = after.turn;                 // 0 / 1
  const remaining     = turn === 0 ?
                        after.remainingTime0 : after.remainingTime1;
  const turnStart     = after.turnStartTime;
  if (!turnStart) return null;                      // clock not started yet

  const elapsedSecs   =
        (Date.now() - turnStart.toMillis()) / 1000;
  const timeLeft      = remaining - elapsedSecs;

  if (timeLeft > 0) return null;                   // still time on the clock

  const winnerUid     = turn === 0 ? after.player1 : after.player0;
  console.log(`Timeout detected – winner: ${winnerUid}`);

  return change.after.ref.update({
      winner:   winnerUid,
      status:   "completed",
      reason:   "timeout",
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
});
