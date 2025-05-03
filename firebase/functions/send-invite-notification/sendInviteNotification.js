const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendInviteNotification = functions.firestore
  .document("invitations/{inviteId}")
  .onCreate(async (snap, context) => {
    const invite = snap.data();
    const { to, fromNickname } = invite;

    if (!to || !fromNickname) {
      console.log("❌ Missing required fields in invite:", invite);
      return null;
    }

    try {
      // 🔍 Get recipient's FCM token
      const userDoc = await admin.firestore().collection("users").doc(to).get();
      const fcmToken = userDoc.get("fcmToken");

      if (!fcmToken) {
        console.log(`⚠️ No FCM token found for user ${to}`);
        return null;
      }

      const payload = {
        notification: {
          title: "Game Invitation",
          body: `${fromNickname} invited you to play!`,
        },
        data: {
          type: "invite",
          fromNickname,
        },
        token: fcmToken,
      };

      const response = await admin.messaging().send(payload);
      console.log("✅ Notification sent:", response);
      return null;
    } catch (error) {
      console.error("❌ Error sending notification:", error);
      return null;
    }
  });
