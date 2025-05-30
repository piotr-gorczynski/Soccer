const functions = require("firebase-functions");
const admin     = require("firebase-admin");
admin.initializeApp();
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
    
  // --- fallback logic ---
  const turnStart = after.turnStartTime ?? after.updatedAt;
  // If turnStartTime is not set, use updatedAt as fallback
  if (!turnStart) {
    console.log(`No turnStartTime in match ${context.params.matchId}`);
    return null;
  }
  // ----------------------

  const elapsedSecs   =
        (Date.now() - turnStart.toMillis()) / 1000;
  const timeLeft      = remaining - elapsedSecs;

  if (timeLeft > 0) return null;                   // still time on the clock

  const winnerUid     = turn === 0 ? after.player1 : after.player0;
  console.log(`Timeout detected in match ${context.params.matchId} – winner: ${winnerUid}`);

  const remainingField = turn === 0 ? "remainingTime0" : "remainingTime1";
  return change.after.ref.update({
      winner:   winnerUid,
      status:   "completed",
      reason:   "timeout",
      [remainingField]: 0, // ⏱ hard-set elapsed clock to 0
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
});
