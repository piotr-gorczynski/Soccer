const functions = require("firebase-functions");
const admin     = require("firebase-admin");

admin.initializeApp();

exports.sendInviteNotification = functions.firestore
  .document("invitations/{inviteId}")
  .onCreate(async (snap, context) => {
    const invite       = snap.data();
    const { to, fromNickname } = invite;

    if (!to || !fromNickname) {
      console.log("❌ Missing required fields in invite:", invite);
      return null;
    }

    // 🔍 Look up the user's FCM token
    const userDoc = await admin.firestore()
      .collection("users")
      .doc(to)
      .get();

    const fcmToken = userDoc.get("fcmToken");
    if (!fcmToken) {
      console.log(`⚠️ No FCM token for user ${to}`);
      return null;
    }

    // ⚙️ Build a data-only message
    const message = {
      token: fcmToken,

      // Everything goes into data
      data: {
        type:         "invite",
        fromNickname,                   // e.g. "Alice"
        title:        "Game Invitation",
        body:         `${fromNickname} invited you to play!`
      },

      // Ensure high-priority delivery on Android
      android: {
        priority: "high"
      }
    };

    try {
      const response = await admin.messaging().send(message);
      console.log("✅ Data-only message sent:", response);
    } catch (err) {
      console.error("❌ Error sending data-only message:", err);
    }

    return null;
  });
