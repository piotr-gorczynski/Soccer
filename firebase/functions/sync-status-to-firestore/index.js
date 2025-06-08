const functions = require("firebase-functions");
const admin     = require("firebase-admin");

// Pull in any config the CLI injected, or fall back to empty
let firebaseConfig;
try {
  firebaseConfig = JSON.parse(process.env.FIREBASE_CONFIG);
} catch (e) {
  firebaseConfig = {};
}

// If we didn’t get a databaseURL in that config, build one from the project ID
if (!firebaseConfig.databaseURL) {
  const pid =
    process.env.GCLOUD_PROJECT ||
    (process.env.FIREBASE_CONFIG && JSON.parse(process.env.FIREBASE_CONFIG).projectId);
  firebaseConfig.databaseURL = `https://${pid}.firebaseio.com`;
}

// Initialize the Admin SDK with the completed config
admin.initializeApp(firebaseConfig);

// Whenever /status/{uid} changes in RTDB, mirror it into Firestore.users.{uid}.online
exports.syncStatusToFirestore = functions.database
  .ref("/status/{uid}")
  .onWrite(async (change, context) => {
    const uid    = context.params.uid;
    const status = change.after.val();              // "online" or "offline"
    const isOnline = status === "online";           // convert to boolean

    const userRef = admin.firestore().collection("users").doc(uid);
    await userRef.set({ online: isOnline }, { merge: true });

    return null;
  });
