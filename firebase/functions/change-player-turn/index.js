// functions/index.js  (Gen 1 – Node 18)
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();
const db        = admin.firestore();

/* ── shared business logic ─────────────────────────────── */
async function handleMove(snap) {
  const move     = snap.data();
  const nextTurn = move?.p;
  if (typeof nextTurn !== 'number') return null;

  const matchRef = snap.ref.parent.parent;        // …/matches/{matchId}
  if (!matchRef) return null;

  await db.runTransaction(async tx => {
    const matchSnap = await tx.get(matchRef);
    if (!matchSnap.exists) return;

    const match = matchSnap.data();
    if (nextTurn === match.turn) return;          // no change → exit

    const playerIdx     = match.turn;            // 0 or 1
    const prevRemaining = playerIdx === 0 ? match.remainingTime0
                                          : match.remainingTime1;

    const startedAt = match.turnStartTime ?? match.updatedAt;
    if (!startedAt) return;                      // guard corrupt data

    const elapsedSecs = (Date.now() - startedAt.toMillis()) / 1000;
    const newRemaining = Math.max(prevRemaining - elapsedSecs, 0);

    tx.update(matchRef, {
      [`remainingTime${playerIdx}`]: Math.floor(newRemaining),
      turn:          nextTurn,
      turnStartTime: null,
      updatedAt:     admin.firestore.FieldValue.serverTimestamp()
    });
  });

  return null;
}

/* ── 1️⃣  friendlies ───────────────────────────────────── */
exports.changePlayerTurn = functions.firestore
  .document('matches/{matchId}/moves/{moveId}')
  .onCreate(handleMove);

/* ── 2️⃣  tournament matches ───────────────────────────── */
exports.changePlayerTurnTournament = functions.firestore
  .document('tournaments/{tid}/matches/{matchId}/moves/{moveId}')
  .onCreate(handleMove);
