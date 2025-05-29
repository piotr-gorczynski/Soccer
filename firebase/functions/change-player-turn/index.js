// functions/index.js
const functions = require("firebase-functions");
const admin     = require("firebase-admin");
admin.initializeApp();
const db        = admin.firestore();

/**
 * When a move hands control to the other player,
 * debit the previous player’s clock and clear turnStartTime.
 *
 * Path: matches/{matchId}/moves/{moveId}
 */
exports.changePlayerTurn = functions.firestore
  .document("matches/{matchId}/moves/{moveId}")
  .onCreate(async (snap, context) => {

  const matchRef = snap.ref.parent.parent;
  if (!matchRef) return null;

  await db.runTransaction(async tx => {
    const matchSnap = await tx.get(matchRef);
    if (!matchSnap.exists) return;

    const match = matchSnap.data();
    const playerIdx     = match.turn;                               // 0 or 1
    const prevRemaining = playerIdx === 0
                          ? match.remainingTime0
                          : match.remainingTime1;

    const startedAt     = match.turnStartTime ?? match.updatedAt;
    if (!startedAt) return;                                         // guard
    const elapsedSecs   = (Date.now() - startedAt.toMillis()) / 1000;
    const newRemaining  = Math.max(prevRemaining - elapsedSecs, 0);

    const updates = {
      [`remainingTime${playerIdx}`]: Math.floor(newRemaining),
      turn:            1 - playerIdx,
      turnStartTime:   null,
      updatedAt:       admin.firestore.FieldValue.serverTimestamp()
    };

    tx.update(matchRef, updates);
  });

  return null;
});
