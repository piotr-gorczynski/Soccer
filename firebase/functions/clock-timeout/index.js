const functions = require("firebase-functions");
const admin     = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Common handler: decides a game on clock timeout.
 * Works for documents at `/matches/{matchId}` and
 * `/tournaments/{tid}/matches/{matchId}`.
 */
async function handleClockTimeout(change, context) {
  const before = change.before.data();
  const after  = change.after.data();

  // Only act while match is active and undecided
  if (after.status !== "active" || after.winner) return null;

  const turn       = after.turn;                        // 0 / 1
  const remaining  = turn === 0 ? after.remainingTime0
                                : after.remainingTime1;

  // --- fallback logic ---
  const turnStart  = after.turnStartTime ?? after.updatedAt;
  if (!turnStart) {
    console.log(`No turnStartTime in match ${context.params.matchId}`);
    return null;
  }
  // ----------------------

  const elapsedSecs = (Date.now() - turnStart.toMillis()) / 1000;
  const timeLeft    = remaining - elapsedSecs;
  if (timeLeft > 0) return null;                        // still time on the clock

  const winnerUid   = turn === 0 ? after.player1 : after.player0;
  const remainingField = turn === 0 ? "remainingTime0" : "remainingTime1";

  console.log(
    `Timeout detected in match ${context.params.matchId} – winner: ${winnerUid}`
  );

  return change.after.ref.update({
    winner:       winnerUid,
    status:       "completed",
    reason:       "timeout",
    [remainingField]: 0,                                // ⏱ set elapsed clock to 0
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}

/**
 * Trigger for *top-level* matches collection: /matches/{matchId}
 */
exports.clockTimeoutMatches = functions.firestore
  .document("matches/{matchId}")
  .onUpdate(handleClockTimeout);

/**
 * Trigger for *tournament* matches sub-collection:
 * /tournaments/{tid}/matches/{matchId}
 */
exports.clockTimeoutTournamentMatches = functions.firestore
  .document("tournaments/{tid}/matches/{matchId}")
  .onUpdate(handleClockTimeout);
