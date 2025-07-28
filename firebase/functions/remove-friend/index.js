const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.removeFriend = functions
  .region('us-central1')
  .https.onCall(async (data, context) => {
    const uid = data?.userId;
    const friendId = data?.friendId;

    if (!context.auth || !context.auth.uid) {
      throw new functions.https.HttpsError('unauthenticated', 'Login required');
    }

    if (!uid || !friendId) {
      throw new functions.https.HttpsError('invalid-argument', 'Missing parameters');
    }

    if (uid !== context.auth.uid) {
      throw new functions.https.HttpsError('permission-denied', 'Invalid userId');
    }

    const ref = admin.firestore()
      .collection('users').doc(uid)
      .collection('friends').doc(friendId);

    const existing = await ref.get();
    if (!existing.exists) {
      throw new functions.https.HttpsError('not-found', 'Friend not found');
    }

    await ref.delete();

    return { friendId };
  });
