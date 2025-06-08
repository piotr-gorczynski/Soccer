// index.js  – Node 18/20, CommonJS
const functions = require("firebase-functions/v1");          // v1 (1st-gen)
const admin     = require("firebase-admin");
admin.initializeApp();

exports.syncStatusToFirestore = functions.database
  .ref("/status/{uid}")
  .onWrite(async (change, context) => {
    const status = change.after.val();        // {state:'online', last_changed: …}
    const isOnline = status?.state === "online";

    await admin
      .firestore()
      .doc(`users/${context.params.uid}`)
      .set(
        { online: isOnline, last_changed: admin.firestore.FieldValue.serverTimestamp() },
        { merge: true }
      );
  });
