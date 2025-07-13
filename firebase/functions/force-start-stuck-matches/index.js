// functions/force-start-stuck-matches/index.js
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();

exports.forceStartStuckMatches = functions           // 1st-gen CF, Node 18
  .region('us-central1')
  .pubsub.schedule('* * * * *')                      // every minute
  .timeZone('Etc/UTC')                               // keep it simple
  .onRun(async () => {

    const cutoff   = Date.now() - 60_000;            // 60 s ago
    const cutoffTs = admin.firestore.Timestamp.fromMillis(cutoff);

    // ⚠️ single “inequality” field is updatedAt; equality filters are allowed
    const snap = await db.collection('matches')
      .where('status',        '==', 'active')
      .where('turnStartTime', '==', null)
      .where('updatedAt',     '<',  cutoffTs)
      .limit(500)                                 // stay under 500 writes/run
      .get();

    if (snap.empty) return null;                  // nothing to do 🙂

    const batch = db.batch();
    snap.forEach(doc => {
      batch.update(doc.ref, {
        turnStartTime : admin.firestore.FieldValue.serverTimestamp(),
        updatedAt     : admin.firestore.FieldValue.serverTimestamp(),
      });
    });
    await batch.commit();
    console.log(`⏩ Forced-started ${snap.size} stuck match(es).`);
    return null;
  });
