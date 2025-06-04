// functions/index.js
const functions = require("firebase-functions");
const admin     = require("firebase-admin");
admin.initializeApp();          // same boot-strap you already use
const db        = admin.firestore();

/* ─────────────────────────────────────────────
 *  1.  Callable: joinTournament
 *      data = { tournamentId: "…" }
 * ────────────────────────────────────────────*/
exports.joinTournament = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  const tid = data?.tournamentId;

  if (!uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "You must be logged in to join a tournament."
    );
  }
  if (!tid) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Parameter `tournamentId` is required."
    );
  }

  const tRef = db.collection("tournaments").doc(tid);
  const pRef = tRef.collection("participants").doc(uid);

  await db.runTransaction(async tx => {
    const tSnap = await tx.get(tRef);
    if (!tSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Tournament not found.");
    }

    const t = tSnap.data();

    if (t.status !== "registering") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Sign-ups are closed for this tournament."
      );
    }

    if (t.participantsCount >= t.maxParticipants) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Tournament is already full."
      );
    }

    /* 1️⃣  write player record */
    tx.set(pRef, {
      seed: Math.random(),                               // used for bracket seeding
      joinedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    /* 2️⃣  bump the live counter atomically */
    tx.update(tRef, {
      participantsCount: admin.firestore.FieldValue.increment(1)
    });
  });

  return { ok: true };             // → Android sees { data: { ok:true } }
});


