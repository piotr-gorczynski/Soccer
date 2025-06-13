const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

const ACTIVE_WINDOW_MS  = 35 * 60 * 1000; // 35 min
const OFFLINE_WINDOW_MS = 2  * 60 * 1000; // 2  min  ← NEW

exports.expirePresence = functions
  .region('us-central1')
  .pubsub.schedule('every 5 minutes')
  .timeZone('Europe/Warsaw')
  .onRun(async () => {
    const db = admin.firestore();
    const now = Date.now();

    /* ─────────────────────────────────────────────────────────────
     * 1) Flip  online → false  if last_changed  > 2 min ago
     * ──────────────────────────────────────────────────────────── */
    const cutoffOnline = now - OFFLINE_WINDOW_MS;

    const staleOnlineSnap = await db.collection('users')
      .where('online', '==', true)
      .where('last_changed', '<', cutoffOnline)
      .get();

    if (!staleOnlineSnap.empty) {
      const batch = db.batch();
      staleOnlineSnap.forEach(doc => batch.update(doc.ref, { online: false }));
      await batch.commit();
      console.log(`Marked ${staleOnlineSnap.size} user(s) offline`);
    } else {
      console.log('No users to mark offline this run');
    }

    /* ─────────────────────────────────────────────────────────────
     * 2) Flip  active → false  if last_changed  > 35 min ago
     * ──────────────────────────────────────────────────────────── */
    const cutoffActive = now - ACTIVE_WINDOW_MS;

    const staleActiveSnap = await db.collection('users')
      .where('active', '==', true)
      .where('last_changed', '<', cutoffActive)
      .get();

    if (!staleActiveSnap.empty) {
      const batch = db.batch();
      staleActiveSnap.forEach(doc => batch.update(doc.ref, { active: false }));
      await batch.commit();
      console.log(`Marked ${staleActiveSnap.size} user(s) inactive`);
    } else {
      console.log('No users to mark inactive this run');
    }

    return null;
  });
