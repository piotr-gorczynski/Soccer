const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const INACTIVITY_WINDOW_MS = 20 * 60 * 1000; // 20 minutes

exports.expirePresence = functions
  .region('us-central1')
  .pubsub.schedule('every 5 minutes')
  .timeZone('Europe/Warsaw')
  .onRun(async () => {
    const rtdb = admin.database();
    const now = Date.now();

    const statusSnap = await rtdb.ref('status').once('value');

    if (!statusSnap.exists()) {
      console.log('ℹ️ No users found in /status');
      return null;
    }

    const updates = {};
    let count = 0;

    statusSnap.forEach(child => {
      const uid = child.key;
      const data = child.val();
      const state = data.state;
      const lastHeartbeat = data.last_heartbeat;

      if (
        state === 'online' &&
        typeof lastHeartbeat === 'number' &&
        lastHeartbeat < now - INACTIVITY_WINDOW_MS
      ) {
        updates[uid] = {
          state: 'offline',
          last_heartbeat: 0
        };
        count++;
      }
    });

    if (count > 0) {
      await rtdb.ref('status').update(updates);
      console.log(`✅ Marked ${count} stale user(s) offline`);
    } else {
      console.log('👌 No stale users found this run');
    }

    return null;
  });
