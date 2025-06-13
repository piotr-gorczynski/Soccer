const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

const ACTIVE_WINDOW_MS = 35 * 60 * 1000; // 35 min

exports.expirePresence = functions
  .region('us-central1')               // use your region
  .pubsub.schedule('every 5 minutes')
  .timeZone('Europe/Warsaw')
  .onRun(async () => {
    const db     = admin.firestore();
    const cutoff = Date.now() - ACTIVE_WINDOW_MS;
    console.log('expirePresence started, cutoff =', cutoff);

    try {
      const staleSnap = await db.collection('users')
        .where('active', '==', true)
        .where('last_changed', '<', cutoff)   // If last_changed is a Timestamp,
                                             // replace cutoff with Timestamp.fromMillis(...)
        .get();

      console.log('stale documents found:', staleSnap.size);

      if (staleSnap.empty) return null;

      const batch = db.batch();
      staleSnap.forEach(doc => batch.update(doc.ref, { active: false }));
      await batch.commit();

      console.log(`Marked ${staleSnap.size} user(s) inactive`);
    } catch (err) {
      console.error('expirePresence failed:', err);
    }
    return null;
  });
