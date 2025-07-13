// functions/force-start-stuck-matches/index.js
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();

exports.forceStartStuckMatches = functions
  .region('us-central1')
  .pubsub
  .schedule('every 1 minutes')        // ← fires once a minute
  .timeZone('Europe/Warsaw')          // ← matches your other jobs
  .onRun(async () => {

    const cutoff   = Date.now() - 60_000;                       // 60 s ago
    const cutoffTs = admin.firestore.Timestamp.fromMillis(cutoff);

    const snap = await db.collection('matches')
      .where('status',        '==', 'active')
      .where('turnStartTime', '==', null)
      .where('updatedAt',     '<',  cutoffTs)
      .limit(500)
      .get();

    if (snap.empty) return null;           // nothing stuck, exit quickly

    const batch = db.batch();
    snap.forEach(doc => {
      batch.update(doc.ref, {
        turnStartTime: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt    : admin.firestore.FieldValue.serverTimestamp(),
      });
    });
    await batch.commit();
    console.log(`⏩ Forced-started ${snap.size} stuck match(es).`);
    return null;
  });
