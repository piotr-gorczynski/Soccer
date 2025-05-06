const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

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

  const matchRef = db.collection("matches").doc();
  const matchData = {
    player0: invite.from,
    player1: invite.to,
    status: "active",
    turn: 0,
    winner: null,
    invitationId: invitationId, // 🔧 added line    
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await db.runTransaction(async tx => {
    tx.update(inviteRef, { status: "accepted" });
    tx.set(matchRef, matchData);

    // 🔸 Add initial move (middle of field, p = 0)
    const fieldHalfWidth  = /* use same integer as your Android resources */;
    const fieldHalfHeight = /* likewise */;
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

  console.log("✅ Match created successfully with ID:", matchRef.id);

  return { matchId: matchRef.id };
});
