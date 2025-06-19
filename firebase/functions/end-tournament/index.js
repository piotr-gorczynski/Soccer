// functions/end-tournaments.js
const functions = require('firebase-functions');
const admin     = require('firebase-admin');

// Only initialize if not already initialized
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const { Timestamp } = admin.firestore;

exports.endTournament = functions.pubsub
  .schedule('every 1 minutes')
  .timeZone('Europe/Warsaw')
  .onRun(async () => {
    const now = Timestamp.now();

    const runningSnap = await db.collection('tournaments')
      .where('status', '==', 'running')
      .get();

    if (runningSnap.empty) {
      console.log('No running tournaments found.');
      return null;
    }

    const toEnd = runningSnap.docs.filter(doc => {
      const deadline = doc.get('matchesDeadline');
      return deadline && deadline.toMillis() <= now.toMillis();
    });

    if (toEnd.length === 0) {
      console.log('No tournaments to end.');
      return null;
    }

    console.log(`Ending ${toEnd.length} tournament(s)…`);

    const batch = db.batch();
    for (const doc of toEnd) {
      batch.update(doc.ref, {
        status: 'ended',
        endedAt: now,
      });
    }

    await batch.commit();
    console.log('Tournaments marked as ended.');

    return null;
  });
