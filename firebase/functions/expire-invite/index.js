// functions/expire-invite/index.js
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

exports.expireInvite = functions
  .region('us-central1')
  .https.onCall(async (data, context) => {

    /* ── basic guards ───────────────────────────────────────── */
    const uid          = context.auth?.uid;
    const invitationId = data?.invitationId;

    if (!uid)
      throw new functions.https.HttpsError('unauthenticated', 'Login required.');

    if (!invitationId)
      throw new functions.https.HttpsError('invalid-argument', 'invitationId missing.');

    const inviteRef = db.collection('invitations').doc(invitationId);

    /* ── atomic expire inside a transaction ─────────────────── */
    await db.runTransaction(async tx => {
      const snap = await tx.get(inviteRef);

      if (!snap.exists)
        throw new functions.https.HttpsError('not-found', 'Invitation does not exist.');

      const invite = snap.data();

      // Only a party to the invite may expire it (from or to)
      if (invite.from !== uid && invite.to !== uid)
        throw new functions.https.HttpsError(
          'permission-denied', 'Only an invite participant may expire this invite.');

      if (invite.status !== 'pending') {
        // Already accepted, cancelled, or expired – nothing to do.
        return;
      }

      tx.update(inviteRef, { status: 'expired' });
    });

    return { ok: true };
  });
