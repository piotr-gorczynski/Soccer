const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { findPromotion } = require("./promotion");
initializeApp();

// Deliberately public; this endpoint has no write path and ignores caller input.
// Cache only this small public projection, never an Auth/Firestore user record.
let cached;
let refreshedAt = 0;
let refresh;
exports.getBangladeshTournamentPromotion = onCall({
  region: "us-central1", invoker: "public", maxInstances: 2, timeoutSeconds: 15
}, async () => {
  const now = Date.now();
  try {
    if (cached === undefined || now - refreshedAt >= 60_000) {
      if (!refresh) refresh = findPromotion(getFirestore(), now).then(value => {
        cached = value; refreshedAt = Date.now();
      }).finally(() => { refresh = null; });
      await refresh;
    }
    const serverTimeMillis = Date.now();
    const tournaments=cached.filter(offer => offer.registrationDeadlineMillis > serverTimeMillis);
    return {
      // Keep the single-offer field for already installed clients.
      tournament: tournaments[0] || null,
      tournaments,
      serverTimeMillis
    };
  } catch (_) {
    // No exception messages, account identifiers or caller payloads in diagnostics.
    throw new HttpsError("unavailable", "Tournament promotion is temporarily unavailable.");
  }
});
