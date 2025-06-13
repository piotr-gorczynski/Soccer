/**
 * Cloud Function: expirePresence
 * Marks users inactive if their last_heartbeat/last_changed
 * is > 20 minutes old.
 *
 * Deploy with:
 *   firebase deploy --only functions:expirePresence          # 1st-gen
 *   or
 *   gcloud functions deploy expirePresence --gen2 ...        # 2nd-gen
 */
const functions = require('firebase-functions');
const admin     = require('firebase-admin');

admin.initializeApp();

// 20-minute sliding window
const ACTIVE_WINDOW_MS = 20 * 60 * 1000;          // 20 × 60 × 1000

exports.expirePresence = functions.pubsub
  .schedule('every 5 minutes')                    // Cloud Scheduler cron
  .timeZone('Europe/Warsaw')                     // keep it local
  .onRun(async () => {
    const cutoff = Date.now() - ACTIVE_WINDOW_MS;

    // Select only documents that can possibly flip
    const snapshot = await admin.firestore()
      .collection('users')
      .where('active', '==', true)
      .where('last_changed', '<', cutoff)
      .get();

    if (snapshot.empty) {
      console.log('No stale sessions — nothing to do.');
      return null;
    }

    // Batch the updates to stay inside the 500-ops limit
    const batch = admin.firestore().batch();
    snapshot.forEach(doc => batch.update(doc.ref, { active: false }));
    await batch.commit();

    console.log(`Marked ${snapshot.size} user(s) inactive`);
    return null;
  });
