const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();
const initialTimeSeconds = 300;  // 5 minutes

exports.acceptInvite = functions.https.onCall(async (data, context) => {
  console.log("----- acceptInvite called -----");

  const uid = context.auth?.uid;
  const invitationId = data.invitationId;

  if (!context.auth) {
    console.error("❌ No auth context provided!");
    throw new functions.https.HttpsError("unauthenticated", "User must be logged in");
  }

  console.log("✅ Authenticated user UID:", uid);

  if (!invitationId) {
    console.error("❌ Invitation ID missing!");
    throw new functions.https.HttpsError("invalid-argument", "Invitation ID is required");
  }

  console.log("🔍 Looking for invitation with ID:", invitationId);

  const inviteRef = db.collection("invitations").doc(invitationId);
  const inviteSnap = await inviteRef.get();

  if (!inviteSnap.exists) {
    console.error("❌ Invitation not found:", invitationId);
    throw new functions.https.HttpsError("not-found", "Invitation not found");
  }

  const invite = inviteSnap.data();

  if (invite.to !== uid) {
    console.error(`❌ Invitation was sent to ${invite.to}, but ${uid} is trying to accept it.`);
    throw new functions.https.HttpsError("permission-denied", "Not your invitation");
  }

  if (invite.status !== "pending") {
    console.error("❌ Invitation already handled. Current status:", invite.status);
    throw new functions.https.HttpsError("failed-precondition", "Invitation already handled");
  }

  console.log("✅ Creating match for players:", invite.from, "vs", invite.to);

  const tid = invite.tournamentId || null;                              // ➊ read once
  const matchPath = invite.matchPath     || null;
  console.log("🔍 Tournament ID (if any):", tid);
  let matchRef;                                          // ➋ choose path
  if (matchPath) {                                  // tournament – doc already exists
    matchRef = db.doc(matchPath);
    const snap = await matchRef.get();
    if (!snap.exists) {
      console.error("❌ Scheduled match document is missing:", matchPath);
      throw new functions.https.HttpsError("not-found",
        "Scheduled match document is missing");
    }
  } else {                                          // friendly – create new doc
    matchRef = db.collection("matches").doc();
  }
 
  const matchData = {
    player0: invite.from,
    player1: invite.to,
    status: "active",
    turn: 0,
    winner: null,
    invitationId,
    tournamentId: tid,            // null for friendlies, redundant for nested
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    // ⏱ Added for chess-style clocks
    remainingTime0: initialTimeSeconds,
    remainingTime1: initialTimeSeconds,
    turnStartTime: null  // ⏱ initially null, set by client later        
  };

  await db.runTransaction(async tx => {
    tx.update(inviteRef, { status: "accepted" });
    if (matchPath) {                             // tournament – just patch
      tx.update(matchRef, {
        status:        "active",
        invitationId,
        updatedAt:     admin.firestore.FieldValue.serverTimestamp(),
        remainingTime0: initialTimeSeconds,
        remainingTime1: initialTimeSeconds,
        turn:           0,
        turnStartTime:  null
      });
    } else {                                     // friendly – create new doc
      tx.set(matchRef, matchData);
+   }

    // 🔸 Add initial move (middle of field, p = 0)
    const fieldHalfWidth  = 3 /* use same integer as your Android resources */;
    const fieldHalfHeight = 4 /* likewise */;
    const initialMove = {
      x: fieldHalfWidth,
      y: fieldHalfHeight,
      p: 0,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    };
    tx.set(
      matchRef.collection("moves").doc(), 
      initialMove
    );    
  });

  // 📨 Fetch inviter's FCM token and send a push
  const fromUserDoc = await db.collection("users").doc(invite.from).get();
  const fcmToken = fromUserDoc.get("fcmToken");

  if (fcmToken) {
    const message = {
      token: fcmToken,
      data: {
        type: "start",
        matchPath: matchRef.path,           // 🔑 send full path
        fromNickname: invite.toNickname || "Your opponent",  // optional
      },
      notification: {
        title: "Game Started!",
        body: `${invite.toNickname || "Your opponent"} has accepted your invitation.`,
      }
    };

    await admin.messaging().send(message);
    console.log("✅ Push sent to inviter (player0)");
  } else {
    console.warn("⚠️ No FCM token found for inviter:", invite.from);
  }
  console.log("✅ Match created successfully with ID:", matchRef.id);

  return { matchPath: matchRef.path };      // 🔑 callable returns same path
});
