const functions = require("firebase-functions");
const admin     = require("firebase-admin");

admin.initializeApp();

exports.sendInviteNotification = functions.firestore
  .document('invitations/{inviteId}')
  .onCreate(async (snap) => {
    const { from, to } = snap.data();        // no nickname anymore

    // Look up both users in parallel
    const [fromDoc, toDoc] = await Promise.all([
      admin.firestore().doc(`users/${from}`).get(),
      admin.firestore().doc(`users/${to}`).get()
    ]);

    const fromNickname = fromDoc.get('nickname') ?? 'Someone';
    const fcmToken     =  toDoc.get('fcmToken');

    if (!fcmToken) return null;

    const message = {
      token: fcmToken,
      data : {
        type:  'invite',
        fromNickname,
        title: 'Game Invitation',
        body:  `${fromNickname} invited you to play!`
      },
      android: { priority: 'high' }
    };

    return admin.messaging().send(message);
});