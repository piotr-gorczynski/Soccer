const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { cleanupDeletedAccount } = require('./deletion');
admin.initializeApp();
const db = admin.firestore();
const cleanup = uid => cleanupDeletedAccount(db, admin.database(), uid, admin.firestore.FieldValue);

exports.removeAccount = functions.region('us-central1').https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  if (!uid) throw new functions.https.HttpsError('unauthenticated', 'Login required');
  try {
    await admin.auth().deleteUser(uid);
  } catch (error) {
    // Retrying a partially completed deletion must still finish the tombstone/cleanup.
    if (error.code !== 'auth/user-not-found') {
      console.error('removeAccount: auth deletion failed', error.code);
      throw new functions.https.HttpsError('internal', 'auth-deletion-failed');
    }
  }
  try {
    await cleanup(uid);
  } catch (error) {
    console.error('removeAccount: cleanup pending; Auth deletion handler will retry', error.code);
    throw new functions.https.HttpsError('internal', 'account-cleanup-pending');
  }
  return { uid };
});

// Durable retry after Auth removal, including removal by scheduled jobs/console.
// Repeated delivery is safe; no historical or payout/legal records are deleted.
exports.onAccountDeleted = functions.region('us-central1')
  .runWith({ failurePolicy: true, timeoutSeconds: 540 })
  .auth.user().onDelete(user => cleanup(user.uid));
