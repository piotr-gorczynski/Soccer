// functions/index.js
const functions = require('firebase-functions');
const admin     = require('firebase-admin');
const TTL_MIN   = 5;                         // <- change to taste
admin.initializeApp();

exports.createInvite = functions
  .region('us-central1')            // keep it with the rest of your back-end
  .https.onCall(async (data, context) => {

    /* ── guards ───────────────────────── */
    const from = context.auth?.uid;
    const to   = data?.toUid;

    if (!from) {
      throw new functions.https.HttpsError('unauthenticated', 'Login required.');
    }
    if (!to) {
      throw new functions.https.HttpsError('invalid-argument', 'Missing toUid.');
    }
    if (from === to) {
      throw new functions.https.HttpsError('failed-precondition',
        'You cannot invite yourself.');
    }

    const tournamentId = data?.tournamentId || null;
    const matchPath    = data?.matchPath    || null;
    const invites = admin.firestore().collection('invitations');
    
    /* ── one-active-invite guarantee ───── */
    const res = await admin.firestore().runTransaction(async tx => {
        const now = admin.firestore.Timestamp.now();          // current server time
        const expireAt  = admin.firestore.Timestamp.fromMillis(now.toMillis() + TTL_MIN * 60_000);
        const conflict = await tx.get(
        invites.where('from', '==', from)
               .where('status', '==', 'pending')
               .where('expireAt', '>', now)       // ← ignore already-expired docs               
               .limit(1)
      );
      if (!conflict.empty) {
        throw new functions.https.HttpsError(
          'failed-precondition', 'You already have another invite pending.');
      }

      const ref = invites.doc();
      tx.set(ref, {
        from, to,
        tournamentId,
        matchPath,
        status    : 'pending',
        expireAt,  // fresh per-call TTL
        createdAt : admin.firestore.FieldValue.serverTimestamp(),
      });

      return { inviteId: ref.id };          // value passed back to the caller
    });

    return res;                             // { inviteId: 'abc123' }
});
