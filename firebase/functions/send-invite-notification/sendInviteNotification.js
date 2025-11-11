const functions = require("firebase-functions");
const admin     = require("firebase-admin");

admin.initializeApp();

exports.sendInviteNotification = functions.firestore
  .document('invitations/{inviteId}')
  .onCreate(async (snap, context) => {
    const inviteId = context.params.inviteId;
    const inviteData = snap.data();
    
    // Log the start of execution
    console.log(`[sendInviteNotification] Processing invitation ${inviteId}`, {
      from: inviteData.from,
      to: inviteData.to
    });

    const { from, to } = inviteData;

    // Validate required fields
    if (!from || !to) {
      console.error(`[sendInviteNotification] Missing required fields in invitation ${inviteId}`, {
        hasFrom: !!from,
        hasTo: !!to
      });
      return null;
    }

    try {
      // Look up both users in parallel
      const [fromDoc, toDoc] = await Promise.all([
        admin.firestore().doc(`users/${from}`).get(),
        admin.firestore().doc(`users/${to}`).get()
      ]);

      // Check if user documents exist
      if (!fromDoc.exists) {
        console.error(`[sendInviteNotification] From user not found: ${from} for invitation ${inviteId}`);
        return null;
      }

      if (!toDoc.exists) {
        console.error(`[sendInviteNotification] To user not found: ${to} for invitation ${inviteId}`);
        return null;
      }

      const fromNickname = fromDoc.get('nickname') ?? 'Someone';
      const fcmToken     = toDoc.get('fcmToken');

      // Check if FCM token exists
      if (!fcmToken) {
        console.log(`[sendInviteNotification] No FCM token for user ${to}, skipping notification for invitation ${inviteId}`);
        return null;
      }

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

      console.log(`[sendInviteNotification] Sending notification for invitation ${inviteId}`, {
        to: to,
        fromNickname: fromNickname
      });

      const result = await admin.messaging().send(message);
      console.log(`[sendInviteNotification] Successfully sent notification for invitation ${inviteId}`, {
        messageId: result
      });
      
      return result;
    } catch (error) {
      // Handle FCM-specific errors
      if (error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/invalid-registration-token') {
        console.error(`[sendInviteNotification] Invalid or expired FCM token for user ${to}, invitation ${inviteId}`, {
          errorCode: error.code,
          errorMessage: error.message
        });
        // Optionally, you could clear the invalid token from the user document here
        return null;
      }

      // Log any other errors with full details
      console.error(`[sendInviteNotification] Failed to send notification for invitation ${inviteId}`, {
        from: from,
        to: to,
        errorCode: error.code,
        errorMessage: error.message,
        errorStack: error.stack
      });
      
      // Re-throw to let Firebase Functions handle the retry logic
      throw error;
    }
});