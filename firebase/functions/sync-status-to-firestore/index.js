/**
 * sync-status-to-firestore
 * ------------------------
 * Listens to /status/{uid} in the Realtime DB and mirrors presence into
 * Firestore users/{uid}:
 *
 *   online : true|false   (socket status)
 *   active : true|false   (online OR last_heartbeat < 20 min)
 *   last_changed : Server TS from RTDB   (for “last-seen”)
 */

/* ------------------------------------------------------------------ */
/* 1. Build a complete FIREBASE_CONFIG programmatically               */
/* ------------------------------------------------------------------ */
let firebaseConfig;
try {
  firebaseConfig = JSON.parse(process.env.FIREBASE_CONFIG || "{}");
} catch {
  firebaseConfig = {};
}

firebaseConfig.projectId =
  firebaseConfig.projectId || process.env.GCLOUD_PROJECT;

firebaseConfig.databaseURL =
  firebaseConfig.databaseURL ||
  `https://${firebaseConfig.projectId}-default-rtdb.firebaseio.com`;

firebaseConfig.storageBucket =
  firebaseConfig.storageBucket ||
  `${firebaseConfig.projectId}.appspot.com`;

// Expose so firebase-functions v1 sees it at require-time
process.env.FIREBASE_CONFIG = JSON.stringify(firebaseConfig);

/* ------------------------------------------------------------------ */
/* 2. Load SDKs after FIREBASE_CONFIG is set                           */
/* ------------------------------------------------------------------ */
const functions = require("firebase-functions/v1");
const admin     = require("firebase-admin");
admin.initializeApp(firebaseConfig);

/* ------------------------------------------------------------------ */
/* 3. Constants                                                       */
/* ------------------------------------------------------------------ */
const ACTIVE_WINDOW_MS = 20 * 60 * 1000;   // 20 min

/* ------------------------------------------------------------------ */
/* 4. Cloud Function                                                  */
/* ------------------------------------------------------------------ */
exports.syncStatusToFirestore = functions.database
  .ref("/status/{uid}")
  .onWrite(async (change, context) => {

    const uid   = context.params.uid;
    const data  = change.after.val() || {};      // might be null on delete

    // 4.1  Determine booleans
    const isOnline = data.state === "online";
    const lastBeat = data.last_heartbeat || 0;
    const activeNow = isOnline || (Date.now() - lastBeat < ACTIVE_WINDOW_MS);

    // 4.2  Copy fields into Firestore
    await admin.firestore().doc(`users/${uid}`).set({
      online       : isOnline,
      active       : activeNow,
      last_changed : data.last_changed || admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    return null;
  });
