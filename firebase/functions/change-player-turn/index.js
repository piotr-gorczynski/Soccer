// functions/index.js
const functions = require("firebase-functions");
const admin     = require("firebase-admin");
admin.initializeApp();
const db        = admin.firestore();

/**
 * If a move hands control to the other player (move.p !== match.turn),
 * debit the previous player’s clock and pass the turn.
 *
 * Path: matches/{matchId}/moves/{moveId}
 */
exports.changePlayerTurn = functions.firestore
  // also catches /tournaments/{tid}/matches/{matchId}/moves/{moveId}
  .document("{parentPath=**}/matches/{matchId}/moves/{moveId}")
  .onCreate(async (snap, context) => {

  const move      = snap.data();               // contains x, y, p, createdAt …
  const nextTurn  = move?.p;                   // who should play next
  if (typeof nextTurn !== "number") return null;

  const matchRef  = snap.ref.parent.parent;
  if (!matchRef) return null;

  await db.runTransaction(async tx => {
    const matchSnap = await tx.get(matchRef);
    if (!matchSnap.exists) return;

    const match = matchSnap.data();

    /*  🔸  Only act if the move actually changes the turn  🔸  */
    if (nextTurn === match.turn) return;       // still the same player -> bounce

    const playerIdx     = match.turn;          // the one who just moved
    const prevRemaining = playerIdx === 0
                          ? match.remainingTime0
                          : match.remainingTime1;

    /*  How long their clock has been running  */
    const startedAt   = match.turnStartTime ?? match.updatedAt;
    if (!startedAt) return;                    // corrupt data guard
    const elapsedSecs = (Date.now() - startedAt.toMillis()) / 1000;
    const newRemaining = Math.max(prevRemaining - elapsedSecs, 0);

    /*  Prepare atomic updates  */
    const updates = {
      [`remainingTime${playerIdx}`]: Math.floor(newRemaining),
      turn:            nextTurn,               // ← hand control to p
      turnStartTime:   null,                   // next player will start it
      updatedAt:       admin.firestore.FieldValue.serverTimestamp()
    };

    tx.update(matchRef, updates);
  });

  return null;
});
