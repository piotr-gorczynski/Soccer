const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

exports.acceptInvite = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  const invitationId = data.invitationId;

  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "User must be logged in");
  }

  if (!invitationId) {
    throw new functions.https.HttpsError("invalid-argument", "Invitation ID is required");
  }

  const inviteRef = db.collection("invitations").doc(invitationId);
  const inviteSnap = await inviteRef.get();

  if (!inviteSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Invitation not found");
  }

  const invite = inviteSnap.data();

  if (invite.to !== uid) {
    throw new functions.https.HttpsError("permission-denied", "Not your invitation");
  }

  if (invite.status !== "pending") {
    throw new functions.https.HttpsError("failed-precondition", "Invitation already handled");
  }

  const matchRef = db.collection("matches").doc();
  const matchData = {
    player0: invite.from,
    player1: invite.to,
    status: "in_progress",
    turn: 0,
    winner: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  await db.runTransaction(async tx => {
    tx.update(inviteRef, { status: "accepted" });
    tx.set(matchRef, matchData);
  });

  return { matchId: matchRef.id };
});
