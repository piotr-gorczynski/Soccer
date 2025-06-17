// functions/index.js
const functions = require("firebase-functions");
const admin     = require("firebase-admin");
admin.initializeApp();
const db        = admin.firestore();

exports.changePlayerTurn = functions.firestore
  .document('{parentPath=**}/matches/{matchId}/moves/{moveId}')
  .onCreate((snap, context) => {

    const move = snap.data();
    const nextTurn = move?.p;
    if (typeof nextTurn !== 'number') return null;

    const matchRef = snap.ref.parent.parent;
    if (!matchRef) return null;

    return db.runTransaction(async tx => {
      const matchSnap = await tx.get(matchRef);
      if (!matchSnap.exists) return;

      const match = matchSnap.data();
      if (nextTurn === match.turn) return;           // bounce → nothing to do

      const playerIdx = match.turn;                  // who just moved
      const prevRemaining = Number.isFinite(
              playerIdx === 0 ? match.remainingTime0 : match.remainingTime1
            ) ? (playerIdx === 0 ? match.remainingTime0 : match.remainingTime1)
              : 0;

      // pick the proper Timestamp object
      const startedAt = (match.turnStartTime instanceof admin.firestore.Timestamp)
                     ? match.turnStartTime
                     : (match.updatedAt   instanceof admin.firestore.Timestamp)
                     ? match.updatedAt
                     : null;
      if (!startedAt) return;                        // corrupt record guard

      const nowSecs     = Date.now() / 1000;
      const elapsedSecs = nowSecs - startedAt.seconds;   // Timestamp → seconds
      const newRemaining = Math.max(prevRemaining - elapsedSecs, 0);

      tx.update(matchRef, {
        [`remainingTime${playerIdx}`]: Math.floor(newRemaining),
        turn:          nextTurn,
        turnStartTime: null,                         // next client starts clock
        updatedAt:     admin.firestore.FieldValue.serverTimestamp()
      });
    });
});
