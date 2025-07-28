const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.addFriend = functions
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

    if (uid === friendId) {
      throw new functions.https.HttpsError('failed-precondition', 'Cannot add yourself');
    }

    const ref = admin.firestore()
      .collection('users').doc(uid)
      .collection('friends').doc(friendId);

    const existing = await ref.get();
    if (existing.exists) {
      throw new functions.https.HttpsError('already-exists', 'Friend already added');
    }

    await ref.set(
      { addedAt: admin.firestore.FieldValue.serverTimestamp() },
      { merge: true }
    );

    return { friendId };
  });
