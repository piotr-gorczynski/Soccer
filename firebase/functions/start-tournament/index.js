const functions = require('firebase-functions/v2');          // 2nd-gen
const { Timestamp } = require('firebase-admin/firestore');
const admin = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();

async function generateRoundRobin(tournamentRef) {
  const now   = Timestamp.now();
  const parts = await tournamentRef.collection('participants').get();
  const ids   = parts.docs.map(d => d.id);

  // If matches already exist → function was retried → skip (idempotent) :contentReference[oaicite:1]{index=1}
  const existing = await tournamentRef.collection('matches').limit(1).get();
  if (!existing.empty) {
    console.log(`Matches already exist for ${tournamentRef.id}, skipping`);
    return;
  }

  const batch = db.batch();
  for (let i = 0; i < ids.length; i++) {
    for (let j = i + 1; j < ids.length; j++) {
      const mRef = tournamentRef.collection('matches').doc();
      batch.set(mRef, {
        playerA: ids[i],
        playerB: ids[j],
        status : 'scheduled',
        createdAt: now
      });
    }
  }
  batch.update(tournamentRef, { status: 'running', startedAt: now });
  await batch.commit();

  console.log(`Round-robin pairs created for ${tournamentRef.id}`);
}


/**
 * Scheduled every hour. Starts any tournaments whose registration window
 * is closed.  Idempotent: if the matches sub-collection already exists,
 * it skips generation.
 */
exports.startTournament = functions.pubsub
  .schedule('every 1 hours')
  .timeZone('UTC')                     // pick your tz if needed
  .onRun(async () => {

    const now = Timestamp.now();
    const snap = await db.collection('tournaments')
      .where('status', '==', 'registering')
      .where('registrationDeadline', '<=', now)
      .get();                                          // :contentReference[oaicite:0]{index=0}

    if (snap.empty) return null;

    for (const doc of snap.docs) {
      const data   = doc.data();
      const tidRef = doc.ref;

      switch (data.format) {
        case 'RoundRobin':
          await generateRoundRobin(tidRef);
          break;

        case 'DoubleElim':
          console.log(`TODO: DoubleElim for ${doc.id}`);
          await tidRef.update({ status: 'running', startedAt: now });
          break;

        case 'Swiss':
          console.log(`TODO: Swiss for ${doc.id}`);
          await tidRef.update({ status: 'running', startedAt: now });
          break;

        default:
          console.warn(`Unknown format ${data.format} in ${doc.id}`);
      }
    }

    return null;
  });
