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

    await admin.firestore().runTransaction(async tx => {
      const own = await tx.get(admin.firestore().collection('users').doc(uid));
      const friend = await tx.get(admin.firestore().collection('users').doc(friendId));
      const existing = await tx.get(ref);
      if (!own.exists || !friend.exists || own.get('accountDeleted') === true || friend.get('accountDeleted') === true) {
        throw new functions.https.HttpsError('failed-precondition', 'Account no longer available');
      }
      // Blocks apply in both directions, including tournament invitations.
      const blockA = await tx.get(admin.firestore().collection('users').doc(uid).collection('blocks').doc(friendId));
      const blockB = await tx.get(admin.firestore().collection('users').doc(friendId).collection('blocks').doc(uid));
      if (blockA.exists || blockB.exists) throw new functions.https.HttpsError('permission-denied', 'player_blocked');

      if (existing.exists) throw new functions.https.HttpsError('already-exists', 'Friend already added');
      tx.set(ref, { addedAt: admin.firestore.FieldValue.serverTimestamp() });
    });

    return { friendId };
  });
