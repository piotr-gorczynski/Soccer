const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

exports.removeAccount = functions
  .region('us-central1')
  .https.onCall(async (data, context) => {
    const uid = context.auth?.uid;
    if (!uid) {
      throw new functions.https.HttpsError('unauthenticated', 'Login required');
    }

    try {
      await admin.auth().deleteUser(uid);
    } catch (err) {
      console.error('removeAccount: auth deletion failed', err);
      throw new functions.https.HttpsError('internal', 'auth-deletion-failed');
    }

    const updates = {
      email: admin.firestore.FieldValue.delete(),
      nickname: '(Account removed)',
      nicknameLowercase: '(account removed)',
      accountDeleted: true,
    };

    try {
      await db.collection('users').doc(uid).update(updates);
    } catch (err) {
      console.error('removeAccount: firestore update failed', err);
      throw new functions.https.HttpsError('internal', 'firestore-update-failed');
    }

    return { uid };
  });
