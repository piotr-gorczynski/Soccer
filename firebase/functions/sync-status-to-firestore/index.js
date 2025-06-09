/**
 * sync-status-to-firestore Cloud Function
 * --------------------------------------
 * 1. Detects writes under /status/{uid} in Realtime DB
 * 2. Mirrors “online/offline” into Firestore users/{uid}.online (boolean)
 * 3. Works in any Firebase project without hard-coding databaseURL
 */

/* ------------------------------------------------------------------ */
/*  ❱❱ 1. Build a complete FIREBASE_CONFIG object programmatically    */
/* ------------------------------------------------------------------ */
let firebaseConfig;
try {
  // CLI injects a minimal JSON string here if it exists
  firebaseConfig = JSON.parse(process.env.FIREBASE_CONFIG || "{}");
} catch {
  firebaseConfig = {};
}

// Ensure we have the project ID
firebaseConfig.projectId =
  firebaseConfig.projectId ||
  process.env.GCLOUD_PROJECT;                     // always set by Cloud Functions

// Ensure we have the databaseURL
firebaseConfig.databaseURL =
  firebaseConfig.databaseURL ||
  `https://${firebaseConfig.projectId}-default-rtdb.firebaseio.com`;

// Optionally fill storageBucket (not required for this function)
firebaseConfig.storageBucket =
  firebaseConfig.storageBucket ||
  `${firebaseConfig.projectId}.appspot.com`;

// Expose the fully-populated config so firebase-functions sees it **now**
process.env.FIREBASE_CONFIG = JSON.stringify(firebaseConfig);

/* ------------------------------------------------------------------ */
/*  ❱❱ 2. Now import firebase-functions (it reads FIREBASE_CONFIG)     */
/* ------------------------------------------------------------------ */
const functions = require("firebase-functions/v1");
const admin     = require("firebase-admin");

admin.initializeApp(firebaseConfig);

/* ------------------------------------------------------------------ */
/*  ❱❱ 3. Cloud Function definition                                    */
/* ------------------------------------------------------------------ */
exports.syncStatusToFirestore = functions.database
  .ref("/status/{uid}")
  .onWrite(async (change, context) => {
    const uid       = context.params.uid;
    const statusStr = change.after.val();               // "online" | "offline"
    const isOnline  = statusStr === "online";

    const userRef = admin.firestore().collection("users").doc(uid);
    await userRef.set({ online: isOnline }, { merge: true });
    return null;
  });
