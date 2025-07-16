const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();

exports.expireStaleMatches = functions
  .region('us-central1')
  .pubsub.schedule('every 1 minutes')
  .timeZone('Europe/Warsaw')
  .onRun(async () => {
    const cutoff   = Date.now() - 5 * 60 * 1000; // 5 minutes ago
    const cutoffTs = admin.firestore.Timestamp.fromMillis(cutoff);

    const snap = await db.collectionGroup('matches')
      .where('status', '==', 'active')
      .where('turnStartTime', '<=', cutoffTs)
      .orderBy('turnStartTime')                 // ensure Firestore uses existing index
      .limit(500)
      .get();

    if (snap.empty) {
      console.log('👌 No stale matches found this run');
      return null;
    }

    const batch = db.batch();
    snap.forEach(doc => {
      const match = doc.data();
      const turn  = match.turn;                 // 0 or 1
      const winnerUid = turn === 0 ? match.player1 : match.player0;
      const remainingField = turn === 0 ? 'remainingTime0' : 'remainingTime1';

      batch.update(doc.ref, {
        winner:     winnerUid,
        status:     'completed',
        reason:     'timeout',
        [remainingField]: 0,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    await batch.commit();
    console.log(`✅ Expired ${snap.size} stale match(es)`);
    return null;
  });
