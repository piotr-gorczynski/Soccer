const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.syncStatusToFirestore = functions.database
  .ref('/status/{uid}')
  .onWrite(async (change, context) => {
    const uid = context.params.uid;
    const status = change.after.val();

    const isOnline = status === 'online';

    return admin.firestore().collection('users').doc(uid).update({
      online: isOnline,
      lastOnline: admin.firestore.FieldValue.serverTimestamp(),
    });
  });
