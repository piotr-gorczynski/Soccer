const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { buildVariantTrackingUpdate } = require("./migration");

admin.initializeApp();
const db = admin.firestore();

exports.trackAppVariant = functions.region("us-central1").https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication is required.");
  }

  const appVariant = data?.appVariant;
  const userRef = db.collection("users").doc(uid);

  try {
    const result = await db.runTransaction(async transaction => {
      const userSnapshot = await transaction.get(userRef);
      const now = admin.firestore.Timestamp.now();
      const update = buildVariantTrackingUpdate(
        userSnapshot.exists ? userSnapshot.data() : {},
        appVariant,
        now
      );
      transaction.set(userRef, update, { merge: true });
      return { migrationDetected: Boolean(update.migrationStatus) };
    });

    console.log("App variant tracked", { uid, appVariant, ...result });
    return { success: true, appVariant, ...result };
  } catch (error) {
    if (error.message === "Invalid app variant.") {
      throw new functions.https.HttpsError("invalid-argument", error.message);
    }
    console.error("Failed to track app variant", { uid, appVariant, error });
    throw new functions.https.HttpsError("internal", "Could not track app variant.");
  }
});
