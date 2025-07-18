const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();

exports.leaveTournament = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  const tid = data?.tournamentId;

  if (!uid) {
    throw new functions.https.HttpsError('unauthenticated', 'You must be logged in.');
  }
  if (!tid) {
    throw new functions.https.HttpsError('invalid-argument', '`tournamentId` is required.');
  }

  const tRef = db.collection('tournaments').doc(tid);
  const pRef = tRef.collection('participants').doc(uid);

  await db.runTransaction(async tx => {
    const pSnap = await tx.get(pRef);
    if (!pSnap.exists) {
      throw new functions.https.HttpsError('failed-precondition', 'You are not registered for this tournament.');
    }

    const tSnap = await tx.get(tRef);
    if (!tSnap.exists) {
      throw new functions.https.HttpsError('not-found', 'Tournament not found.');
    }
    const t = tSnap.data();
    if (t.status !== 'registering') {
      throw new functions.https.HttpsError('failed-precondition', 'Tournament already started.');
    }

    tx.delete(pRef);
    tx.update(tRef, {
      participantsCount: admin.firestore.FieldValue.increment(-1)
    });
  });

  return { ok: true };
});
