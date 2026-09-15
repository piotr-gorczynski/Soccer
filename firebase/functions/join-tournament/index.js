// functions/join-tournament/index.js
const functions = require("firebase-functions");
const admin     = require("firebase-admin");
const {
  getCashEligibilityRequirements,
  buildEligibilityConfirmation
} = require("./eligibility");
admin.initializeApp();                 // same bootstrap you use elsewhere:contentReference[oaicite:1]{index=1}
const db        = admin.firestore();

/**
 * Callable - joinTournament
 *   data = { tournamentId: "abc123" }
 */
exports.joinTournament = functions.https.onCall(async (data, context) => {
  const uid = context.auth?.uid;
  const tid = data?.tournamentId;
  const regulationStatus = data?.regulation;
  const eligibility = data?.eligibility;

  /* ─── 1. Basic guards ─── */
  if (!uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "You must be logged in."
    );
  }
  if (!tid) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "`tournamentId` is required."
    );
  }

  const tRef = db.collection("tournaments").doc(tid);
  const pRef = tRef.collection("participants").doc(uid);

  /* ─── 2. Single transaction ─── */
  await db.runTransaction(async tx => {

    /* 2-a: already joined? */
    const pSnap = await tx.get(pRef);
    if (pSnap.exists) {
      throw new functions.https.HttpsError(
        "already-exists",
        "You are already registered for this tournament."
      );
    }

    /* 2-b: validate tournament state */
    const tSnap = await tx.get(tRef);
    if (!tSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Tournament not found.");
    }
    const t = tSnap.data();

    if (t.status !== "registering") {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Sign-ups are closed."
      );
    }
    if (t.participantsCount >= t.maxParticipants) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Tournament is already full."
      );
    }

    let eligibilityConfirmation = null;
    const regulationId = t.regulation;
    const cashPrizeEnabled = t.prizePool?.enabled === true;
    if (cashPrizeEnabled && (!regulationId || typeof regulationId !== "string")) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Cash-prize tournament has no regulation."
      );
    }

    if (regulationId && typeof regulationId === "string") {
      const regulationRef = db.collection("regulations").doc(regulationId);
      const regulationSnap = await tx.get(regulationRef);
      if (!regulationSnap.exists) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Tournament regulation not found."
        );
      }

      try {
        const requirements = getCashEligibilityRequirements(t, regulationSnap.data());
        if (requirements) {
          eligibilityConfirmation = buildEligibilityConfirmation(
            eligibility,
            regulationId,
            requirements,
            admin.firestore.FieldValue.serverTimestamp()
          );
        }
      } catch (error) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          error.message
        );
      }
    }

    /* 2-c: write participant + increment counter atomically */
    const payload = {
      seed: Math.random(),                              // for bracket seeding
      joinedAt: admin.firestore.FieldValue.serverTimestamp()
    };
    if (regulationStatus) payload.regulation = regulationStatus;
    if (eligibilityConfirmation) {
      payload.eligibilityConfirmation = eligibilityConfirmation;
    }
    tx.set(pRef, payload);
    tx.update(tRef, {
      participantsCount: admin.firestore.FieldValue.increment(1)
    });
  });

  /* ─── 3. Return a tiny success payload ─── */
  return { ok: true };
});
