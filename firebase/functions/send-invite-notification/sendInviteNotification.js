const functions = require("firebase-functions/v1");
const { initializeApp } = require("firebase-admin/app");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { getDatabase } = require("firebase-admin/database");

initializeApp();

const db = getFirestore();
const messaging = getMessaging();
const rtdb = getDatabase();

async function invalidateFcmTarget(userId, targetField, targetValue, fcmErrorType, sendStartedAt) {
  const userRef = db.doc(`users/${userId}`);
  const targetWasInvalidated = await db.runTransaction(async transaction => {
    const currentUser = await transaction.get(userRef);
    if (!currentUser.exists || currentUser.get(targetField) !== targetValue) {
      return false;
    }

    transaction.update(userRef, {
      [targetField]: FieldValue.delete(),
      fcmErrorType,
      fcmErrorDate: FieldValue.serverTimestamp()
    });
    return true;
  });

  if (!targetWasInvalidated || fcmErrorType !== 'NotRegistered') {
    return targetWasInvalidated;
  }

  const statusRef = rtdb.ref(`status/${userId}`);
  const presenceResult = await statusRef.transaction(currentStatus => {
    const lastHeartbeat = Number(currentStatus?.last_heartbeat || 0);
    if (lastHeartbeat > sendStartedAt) {
      return;
    }
    return { ...(currentStatus || {}), state: 'offline', last_heartbeat: 0 };
  });

  console.log(`[sendInviteNotification] Presence invalidation for user ${userId}`, {
    committed: presenceResult.committed,
    sendStartedAt
  });
  return true;
}

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
    const sendStartedAt = Date.now();
    let attemptedTargetField;
    let attemptedTargetValue;

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
        db.doc(`users/${from}`).get(),
        db.doc(`users/${to}`).get()
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

      const fromNickname      = fromDoc.get('nickname') ?? 'Someone';
      const fcmInstallationId = toDoc.get('fcmInstallationId');
      const fcmToken          = toDoc.get('fcmToken');

      if (!fcmInstallationId && !fcmToken) {
        console.log(`[sendInviteNotification] No FCM target for user ${to}, skipping notification for invitation ${inviteId}`);
        return null;
      }

      const message = {
        ...(fcmInstallationId ? { fid: fcmInstallationId } : { token: fcmToken }),
        data : {
          type:  'invite',
          fromNickname,
          title: 'Game Invitation',
          body:  `${fromNickname} invited you to play!`
        },
        android: { priority: 'high' }
      };

      attemptedTargetField = fcmInstallationId ? 'fcmInstallationId' : 'fcmToken';
      attemptedTargetValue = fcmInstallationId || fcmToken;

      console.log(`[sendInviteNotification] Sending notification for invitation ${inviteId}`, {
        to: to,
        fromNickname: fromNickname,
        targetType: fcmInstallationId ? 'fid' : 'legacy-token'
      });

      const result = await messaging.send(message);
      console.log(`[sendInviteNotification] Successfully sent notification for invitation ${inviteId}`, {
        messageId: result
      });
      
      return result;
    } catch (error) {
      // Handle FCM-specific errors
      if (error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/installation-id-not-registered' ||
          error.code === 'messaging/invalid-registration-token') {
        console.error(`[sendInviteNotification] Invalid or expired FCM token for user ${to}, invitation ${inviteId}`, {
          errorCode: error.code,
          errorMessage: error.message
        });
        
        // Store error information in user document
        try {
          const fcmErrorType = error.code === 'messaging/registration-token-not-registered' ||
            error.code === 'messaging/installation-id-not-registered'
            ? 'NotRegistered' 
            : 'InvalidRegistration';

          const invalidated = await invalidateFcmTarget(
            to,
            attemptedTargetField,
            attemptedTargetValue,
            fcmErrorType,
            sendStartedAt
          );
          
          console.log(`[sendInviteNotification] Stored FCM error info for user ${to}`, {
            fcmErrorType: fcmErrorType,
            invalidated
          });
        } catch (updateError) {
          console.error(`[sendInviteNotification] Failed to update user document with FCM error for user ${to}`, {
            errorCode: updateError.code,
            errorMessage: updateError.message
          });
        }
        
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
